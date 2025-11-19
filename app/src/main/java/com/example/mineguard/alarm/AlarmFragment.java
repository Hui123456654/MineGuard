package com.example.mineguard.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.textfield.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 报警管理Fragment
 * 实现实时报警推送、应急策略、报警信息查询和报警详情功能
 */
public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener, 
        FilterDialog.OnFilterChangeListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String PREF_NAME = "alarm_prefs";
    private static final String KEY_ALARM_COUNT = "alarm_count";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1001;

    private RecyclerView recyclerView;
    private TextInputEditText searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> alarmList;
    private List<AlarmItem> filteredList;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    private Ringtone ringtone;
    private SharedPreferences preferences;
    private int currentAlarmCount = 0;

    // 筛选条件
    private String selectedDevice = "";
    private String selectedAlgorithm = "";
    private String selectedScene = "";
    private String selectedArea = "";
    private String selectedStatus = "";
    private String selectedTimeRange = "";

    public AlarmFragment() {
        // Required empty public constructor
    }

    public static AlarmFragment newInstance(String param1, String param2) {
        AlarmFragment fragment = new AlarmFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
        
        initializeServices();
        createNotificationChannel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupSearchView();
        setupSwipeRefresh();
        loadAlarmData();
        
        return view;
    }

    private void initializeServices() {
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // 初始化铃声用于语音提醒
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(requireContext(), alarmUri);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "报警通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("矿山设备报警通知");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        searchView = view.findViewById(R.id.searchView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // 设置筛选按钮点击事件
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        
        // 设置清空筛选按钮点击事件
        view.findViewById(R.id.btnClearFilter).setOnClickListener(v -> clearFilters());
    }

    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        filteredList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(filteredList, this);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(alarmAdapter);
    }

    private void setupSearchView() {
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAlarms(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不需要处理
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAlarmData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadAlarmData() {
        // 生成模拟数据
        generateMockData();
        
        // 应用当前筛选条件
        applyFilters();
        
        // 检查新报警并发送通知
        checkNewAlarms();
    }

    private void generateMockData() {
        alarmList.clear();

        long currentTime = System.currentTimeMillis();

        // 固定的报警数据
        AlarmItem alarm1 = new AlarmItem();
        alarm1.setId("ALARM_1001");
        alarm1.setDeviceName("掘进机A1");
        alarm1.setAlgorithmType("人员检测");
        alarm1.setScene("掘进工作面");
        alarm1.setArea("东区");
        alarm1.setStatus("未处理");
        alarm1.setLevel(AlarmItem.LEVEL_CRITICAL);
        alarm1.setTimestamp(currentTime - 1 * 60 * 60 * 1000); // 1小时前
        alarm1.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm1);

        AlarmItem alarm2 = new AlarmItem();
        alarm2.setId("ALARM_1002");
        alarm2.setDeviceName("采煤机B2");
        alarm2.setAlgorithmType("设备异常");
        alarm2.setScene("采煤工作面");
        alarm2.setArea("西区");
        alarm2.setStatus("处理中");
        alarm2.setLevel(AlarmItem.LEVEL_WARNING);
        alarm2.setTimestamp(currentTime - 3 * 60 * 60 * 1000); // 3小时前
        alarm2.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm2);

        AlarmItem alarm3 = new AlarmItem();
        alarm3.setId("ALARM_1003");
        alarm3.setDeviceName("运输机C3");
        alarm3.setAlgorithmType("瓦斯超标");
        alarm3.setScene("运输巷道");
        alarm3.setArea("南区");
        alarm3.setStatus("已处理");
        alarm3.setLevel(AlarmItem.LEVEL_WARNING);
        alarm3.setTimestamp(currentTime - 5 * 60 * 60 * 1000); // 5小时前
        alarm3.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm3);

        AlarmItem alarm4 = new AlarmItem();
        alarm4.setId("ALARM_1004");
        alarm4.setDeviceName("风机D4");
        alarm4.setAlgorithmType("温度异常");
        alarm4.setScene("通风系统");
        alarm4.setArea("北区");
        alarm4.setStatus("未处理");
        alarm4.setLevel(AlarmItem.LEVEL_WARNING);
        alarm4.setTimestamp(currentTime - 8 * 60 * 60 * 1000); // 8小时前
        alarm4.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm4);

        AlarmItem alarm5 = new AlarmItem();
        alarm5.setId("ALARM_1005");
        alarm5.setDeviceName("水泵E5");
        alarm5.setAlgorithmType("振动异常");
        alarm5.setScene("排水系统");
        alarm5.setArea("中央区");
        alarm5.setStatus("已处理");
        alarm5.setLevel(AlarmItem.LEVEL_WARNING);
        alarm5.setTimestamp(currentTime - 12 * 60 * 60 * 1000); // 12小时前
        alarm5.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm5);

        AlarmItem alarm6 = new AlarmItem();
        alarm6.setId("ALARM_1006");
        alarm6.setDeviceName("掘进机A1");
        alarm6.setAlgorithmType("设备异常");
        alarm6.setScene("掘进工作面");
        alarm6.setArea("东区");
        alarm6.setStatus("未处理");
        alarm6.setLevel(AlarmItem.LEVEL_CRITICAL);
        alarm6.setTimestamp(currentTime - 15 * 60 * 60 * 1000); // 15小时前
        alarm6.setImageRes(R.drawable.placeholder);
        alarmList.add(alarm6);

        // 按时间倒序排列
        alarmList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
    }
    private void filterAlarms(String query) {
        filteredList.clear();
        
        for (AlarmItem alarm : alarmList) {
            boolean matches = true;
            
            // 搜索关键词匹配
            if (!query.isEmpty()) {
                matches = alarm.getDeviceName().toLowerCase().contains(query.toLowerCase()) ||
                         alarm.getAlgorithmType().toLowerCase().contains(query.toLowerCase()) ||
                         alarm.getScene().toLowerCase().contains(query.toLowerCase());
            }
            
            // 应用筛选条件
            if (matches && !selectedDevice.isEmpty()) {
                matches = alarm.getDeviceName().equals(selectedDevice);
            }
            if (matches && !selectedAlgorithm.isEmpty()) {
                matches = alarm.getAlgorithmType().equals(selectedAlgorithm);
            }
            if (matches && !selectedScene.isEmpty()) {
                matches = alarm.getScene().equals(selectedScene);
            }
            if (matches && !selectedArea.isEmpty()) {
                matches = alarm.getArea().equals(selectedArea);
            }
            if (matches && !selectedStatus.isEmpty()) {
                matches = alarm.getStatus().equals(selectedStatus);
            }
            
            if (matches) {
                filteredList.add(alarm);
            }
        }
        
        alarmAdapter.notifyDataSetChanged();
    }

    private void applyFilters() {
        filterAlarms(searchView.getText().toString());
    }

    private void clearFilters() {
        selectedDevice = "";
        selectedAlgorithm = "";
        selectedScene = "";
        selectedArea = "";
        selectedStatus = "";
        selectedTimeRange = "";
        applyFilters();
        Toast.makeText(requireContext(), "筛选条件已清空", Toast.LENGTH_SHORT).show();
    }

    private void showFilterDialog() {
        FilterDialog dialog = FilterDialog.newInstance(
                selectedDevice, selectedAlgorithm, selectedScene, 
                selectedArea, selectedStatus, selectedTimeRange);
        dialog.show(getChildFragmentManager(), "FilterDialog");
    }

    private void checkNewAlarms() {
        int newAlarmCount = getUnprocessedAlarmCount();
        int previousCount = preferences.getInt(KEY_ALARM_COUNT, 0);
        
        if (newAlarmCount > previousCount) {
            // 有新报警
            currentAlarmCount = newAlarmCount - previousCount;
            sendNotification();
            
            // 如果有严重报警，触发应急策略
            if (hasCriticalAlarm()) {
                triggerEmergencyResponse();
            }
        }
        
        // 更新保存的计数
        preferences.edit().putInt(KEY_ALARM_COUNT, newAlarmCount).apply();
    }

    private int getUnprocessedAlarmCount() {
        int count = 0;
        for (AlarmItem alarm : alarmList) {
            if (!alarm.getStatus().equals("已处理")) {
                count++;
            }
        }
        return count;
    }

    private boolean hasCriticalAlarm() {
        for (AlarmItem alarm : alarmList) {
            if (alarm.getLevel() == AlarmItem.LEVEL_CRITICAL && 
                !alarm.getStatus().equals("已处理")) {
                return true;
            }
        }
        return false;
    }

    private void sendNotification() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("新报警通知")
                .setContentText("您有 " + currentAlarmCount + " 条新报警需要处理")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setNumber(currentAlarmCount);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void triggerEmergencyResponse() {
        // 触发振动
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, -1);
        }

        // 播放语音提醒
        if (ringtone != null && !ringtone.isPlaying()) {
            // 循环播放铃声
        }

        Toast.makeText(requireContext(), "严重报警！已触发应急响应", Toast.LENGTH_LONG).show();
    }

    // AlarmAdapter.OnAlarmClickListener 实现
    @Override
    public void onAlarmClick(AlarmItem alarm) {
        // 跳转到报警详情页
        AlarmDetailDialog dialog = AlarmDetailDialog.newInstance(alarm);
        dialog.show(getChildFragmentManager(), "AlarmDetailDialog");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) {
        // 长按显示快速处理选项
        showQuickProcessDialog(alarm);
    }

    private void showQuickProcessDialog(AlarmItem alarm) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("快速处理")
                .setItems(new String[]{"标记为已处理", "标记为处理中", "查看详情"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            alarm.setStatus("已处理");
                            break;
                        case 1:
                            alarm.setStatus("处理中");
                            break;
                        case 2:
                            onAlarmClick(alarm);
                            break;
                    }
                    alarmAdapter.notifyDataSetChanged();
                })
                .show();
    }

    // FilterDialog.OnFilterChangeListener 实现
    @Override
    public void onFilterChanged(String device, String algorithm, String scene, 
                               String area, String status, String timeRange) {
        selectedDevice = device;
        selectedAlgorithm = algorithm;
        selectedScene = scene;
        selectedArea = area;
        selectedStatus = status;
        selectedTimeRange = timeRange;
        applyFilters();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ringtone != null) {
            if (ringtone.isPlaying()) {
                ringtone.stop();
            }
            ringtone = null;
        }
    }
}
