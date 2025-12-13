package com.example.mineguard.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.alarm.adapter.AlarmAdapter;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.alarm.dialog.FilterDialog;
import com.example.mineguard.alarm.dialog.AlarmDetailDialog;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener,
        FilterDialog.OnFilterChangeListener {

    // ================== 请修改这里 ==================

    private static final String CAMERA_NORMAL_IP = "192.168.31.64"; // 异物相机
    private static final String CAMERA_LIMIT_IP  = "192.168.31.65"; // 四超相机
    private static final String CAMERA_COAL_IP   = "192.168.31.66"; // 煤量相机
    // ===============================================

    private static final int LOCAL_PORT = 8080; // 手机监听端口
    private LocalServer localServer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable keepAliveRunnable;

    // UI 变量
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> alarmList = new ArrayList<>();
    private List<AlarmItem> filteredList = new ArrayList<>();

    // 通知相关
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "alarm_channel";

    public AlarmFragment() { }

    public static AlarmFragment newInstance(String param1, String param2) {
        return new AlarmFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化通知渠道
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "报警通知", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 1. 启动本地服务器
        startLocalServer();

        // 2. 启动“心跳”机制：每隔 1 分钟告诉相机一次配置信息，防止相机断电遗忘
        startKeepAlive();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // 初始化列表
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(alarmList, this);
        recyclerView.setAdapter(alarmAdapter);

        // 下拉刷新时，手动触发一次连接相机配置
        // 下拉刷新时，重新配置所有相机
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 1. 配置普通相机
            configureCamera(CAMERA_NORMAL_IP, "/api/recv/alarmInfo", "/server/on/alarm/info/upload/");

            // 2. 配置四超相机
            configureCamera(CAMERA_LIMIT_IP, "/api/recv/fourLimit", "/server/on/four/limit/alarm/upload/");

            // 3. 配置煤量相机
            configureCamera(CAMERA_COAL_IP, "/api/recv/coalWeight", "/server/on/volum/weight/upload/");

            // 延迟 1 秒关闭刷新圈
            handler.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        return view;
    }

    /**
     * 启动本地 HTTP 服务器
     */
    private void startLocalServer() {
        try {
            localServer = new LocalServer(LOCAL_PORT, requireContext());
            localServer.setListener(alarmItem -> {
                // 当 LocalServer 收到数据时，切换到主线程更新 UI
                handler.post(() -> {
                    // 添加到列表顶部
                    alarmList.add(0, alarmItem);
                    alarmAdapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);

                    // 发送通知
                    sendNotification(alarmItem);
                });
            });
            localServer.start();
            Log.i("AlarmFragment", "手机服务器已启动，端口: " + LOCAL_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "服务器启动失败，端口可能被占用", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 配置相机：告诉相机把数据发给手机
     * 参考文档 [cite: 5, 12, 13]
     */
    // 修改 configureCamera 方法，增加 targetUri 参数 [cite: 5]
    private void configureCamera(String deviceIp, String targetUri, String openUrlPath) {
        String myIp = getPhoneIpAddress();
        // === [新增] 打印这行日志 ===
        Log.i("Connect", "正在配置相机 " + deviceIp + "，告诉它回传给: " + myIp);
        if (myIp == null) return;

        OkHttpClient client = new OkHttpClient();

        // 构造参数：告诉相机发到 targetUri 这个路径
        FormBody body = new FormBody.Builder()
                .add("protocol", "http")
                .add("port", String.valueOf(8080)) // LOCAL_PORT
                .add("uri", targetUri)             // 关键：不同的相机发不同的 URI
                .add("extend", deviceIp)           // 把相机IP回传回来做识别
                .build();

        // 开启上传的 URL [cite: 13, 31, 58]
        String url = "http://" + deviceIp + ":5002" + openUrlPath;

        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> Log.e("Connect", "连接相机失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                handler.post(() -> {
                    if (response.isSuccessful()) {
                        Log.i("Connect", "相机配置成功，正在等待报警...");
                    } else {
                        Log.e("Connect", "相机拒绝连接: " + response.code());
                    }
                });
            }
        });
    }

    // 定时任务：每 60 秒发送一次配置指令
    private void startKeepAlive() {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. 配置普通相机 (接口1) [cite: 13]
                configureCamera(CAMERA_NORMAL_IP, LocalServer.URI_ALARM_1, "/server/on/alarm/info/upload/");

                // 2. 配置四超相机 (接口3) [cite: 31]
                configureCamera(CAMERA_LIMIT_IP, LocalServer.URI_ALARM_3, "/server/on/four/limit/alarm/upload/");

                // 3. 配置煤量相机 (接口6) [cite: 58]
                configureCamera(CAMERA_COAL_IP, LocalServer.URI_COAL_6, "/server/on/volum/weight/upload/");

                handler.postDelayed(this, 60 * 1000);
            }
        };
        handler.post(keepAliveRunnable);
    }


    /**
     * 获取手机 IP 地址的工具方法
     */
    private String getPhoneIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void sendNotification(AlarmItem item) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("新报警: " + item.getName())
                .setContentText("时间: " + item.getSolve_time())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

    // 适配器点击事件
    @Override
    public void onAlarmClick(AlarmItem alarm) {
        AlarmDetailDialog.newInstance(alarm).show(getChildFragmentManager(), "detail");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) { }

    @Override
    public void onFilterChanged(String t, String l, String s, String loc) { } // 简化版暂不实现筛选

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (localServer != null) localServer.stop();
        if (keepAliveRunnable != null) handler.removeCallbacks(keepAliveRunnable);
    }
}