package com.example.mineguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mineguard.alarm.AlarmFragment;
import com.example.mineguard.alarm.LocalServer;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.analysis.AnalysisFragment;
import com.example.mineguard.configuration.ConfigurationFragment;
import com.example.mineguard.data.AlarmRepository;
import com.example.mineguard.data.DeviceItem;
import com.example.mineguard.data.DeviceRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Fragment[] fragments = new Fragment[3];
    private int currentIndex = 0;
    private WindowInsetsControllerCompat windowInsetsController;

    private static final int LOCAL_PORT = 8080; // 手机监听端口
    private LocalServer localServer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable keepAliveRunnable;

    // 全局报警数据列表
    private List<AlarmItem> globalAlarmList = new ArrayList<>();

    // 通知相关
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "alarm_channel";

    private AlarmRepository alarmRepository;

    // 接口：用于通知 Fragment 数据更新
    public interface OnAlarmReceivedListener {
        void onNewAlarm(AlarmItem item);
    }
    // 监听器列表
    private List<OnAlarmReceivedListener> listeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 确保仓库初始化
        DeviceRepository.init(getApplicationContext());

        setContentView(R.layout.activity_main);

        windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        alarmRepository = new AlarmRepository(getApplication());

        initNotificationChannel();
        startLocalServer();
        startKeepAlive(); // 启动心跳

        // Fragment 初始化
        fragments[0] = new AnalysisFragment();
        fragments[1] = new AlarmFragment();
        fragments[2] = new ConfigurationFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragments[0])
                .commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int newIndex = -1;
            if (id == R.id.nav_analysis) newIndex = 0;
            else if (id == R.id.nav_alarms) newIndex = 1;
            else if (id == R.id.nav_configuration) newIndex = 2;

            if (newIndex != -1 && newIndex != currentIndex) {
                switchFragment(currentIndex, newIndex);
                currentIndex = newIndex;
                return true;
            }
            return false;
        });

    }

    private void switchFragment(int oldIndex, int newIndex) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment oldFrag = fragments[oldIndex];
        Fragment newFrag = fragments[newIndex];

        if (!newFrag.isAdded()) {
            transaction.hide(oldFrag).add(R.id.fragment_container, newFrag);
        } else {
            transaction.hide(oldFrag).show(newFrag);
        }
        transaction.commit();
    }

    // ================== 核心修改：统一的配置逻辑 ==================

    /**
     * 根据设备类型获取对应的 LocalServer 监听 URI
     */
    private String getLocalUriByType(String deviceType) {
        if (deviceType == null) return "/api/recv/alarmInfo"; // 默认

        switch (deviceType) {
            case "异物相机":
            case "异物":
                return "/api/recv/alarmInfo";
            case "三超相机":
            case "三超":
                return "/api/recv/fourLimit";
            case "煤量相机":
            case "煤量":
                return "/api/recv/coalWeight";
            default:
                return "/api/recv/alarmInfo";
        }
    }

    /**
     * 根据设备类型获取相机端的上传接口路径
     */
    private String getRemotePathByType(String deviceType) {
        if (deviceType == null) return "/server/on/alarm/info/upload/";

        switch (deviceType) {
            case "异物相机":
            case "异物":
                return "/server/on/alarm/info/upload/";
            case "三超相机":
            case "三超":
                return "/server/on/four/limit/alarm/upload/";
            case "煤量相机":
            case "煤量":
                return "/server/on/volum/weight/upload/";
            default:
                return "/server/on/alarm/info/upload/";
        }
    }

    /**
     * 遍历 DeviceRepository 中的所有设备并进行配置 (心跳逻辑)
     */
    private void configureDevicesFromRepository() {
        // 1. 获取最新设备列表
        List<DeviceItem> devices = DeviceRepository.getInstance().getDevices();

        if (devices == null || devices.isEmpty()) {
            Log.w("MainActivity", "设备列表为空，跳过配置");
            return;
        }

        // 2. 遍历列表，配置每一个设备
        for (DeviceItem item : devices) {
            // 跳过没有 IP 或已经标记为离线的设备
            if (item.getIpAddress() == null || item.getIpAddress().isEmpty()) {
                continue;
            }

            // 根据 DeviceType 智能匹配参数
            String localUri = getLocalUriByType(item.getDeviceType());
            String remotePath = getRemotePathByType(item.getDeviceType());

            // 发送配置请求
            configureAlarmCamera(item.getIpAddress(), localUri, remotePath);
        }
    }

    // ================== 公共方法 (供 Fragment 调用) ==================

    /**
     * 手动刷新配置 (ConfigurationFragment 可能调用)
     */
    public void manualRefreshAlarmConfig() {
        configureDevicesFromRepository();
        Toast.makeText(this, "正在刷新所有设备配置...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 注册监听器 (Fragment调用)
     */
    public void addAlarmListener(OnAlarmReceivedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除监听器
     */
    public void removeAlarmListener(OnAlarmReceivedListener listener) {
        listeners.remove(listener);
    }

    /**
     * 获取当前的全局报警列表
     */
    public List<AlarmItem> getGlobalAlarmList() {
        return globalAlarmList;
    }

    // ================== 服务器与网络相关 ==================

    private void startKeepAlive() {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // 每分钟遍历一次数据库中的设备并维持连接
                configureDevicesFromRepository();
                // 60秒后再次执行
                handler.postDelayed(this, 60 * 1000);
            }
        };
        handler.post(keepAliveRunnable);
    }

    private void configureAlarmCamera(String deviceIp, String targetUri, String openUrlPath) {
        String myIp = getPhoneIpAddress();
        if (myIp == null) return;

        OkHttpClient client = new OkHttpClient();
        FormBody body = new FormBody.Builder()
                .add("protocol", "http")
                .add("port", String.valueOf(LOCAL_PORT))
                .add("uri", targetUri)
                .add("extend", deviceIp)
                .build();

        // 默认使用 5002 端口
        String url = "http://" + deviceIp + ":5002" + openUrlPath;

        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Connect", "连接设备失败: " + deviceIp);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Log.e("Connect", "设备拒绝连接: " + deviceIp + " Code:" + response.code());
                } else {
                    Log.i("Connect", "设备连接成功: " + deviceIp);
                }
                response.close();
            }
        });
    }

    private void initNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "报警通知", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startLocalServer() {
        try {
            localServer = new LocalServer(LOCAL_PORT, this);
            localServer.setListener(alarmItem -> {
                handler.post(() -> {
                    // 收到报警 -> 存入数据库
                    alarmRepository.insert(alarmItem);
                    // 发通知
                    sendNotification(alarmItem);
                    // 通知 Fragment
                    for (OnAlarmReceivedListener listener : listeners) {
                        listener.onNewAlarm(alarmItem);
                    }
                });
            });
            localServer.start();
            Log.i("MainActivity", "全局服务器已启动，端口: " + LOCAL_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "服务器启动失败，端口可能被占用", Toast.LENGTH_LONG).show();
        }
    }

    private void sendNotification(AlarmItem item) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("新报警: " + item.getName())
                .setContentText("时间: " + item.getSolve_time())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

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
        } catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localServer != null) localServer.stop();
        if (keepAliveRunnable != null) handler.removeCallbacks(keepAliveRunnable);
    }
}