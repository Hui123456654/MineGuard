package com.example.mineguard.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.mineguard.R;
import com.example.mineguard.alarm.adapter.AlarmAdapter;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.alarm.dialog.AlarmDetailDialog;
import com.example.mineguard.alarm.dialog.FilterDialog;
import com.example.mineguard.data.DeviceItem; // 导入
import com.example.mineguard.data.DeviceViewModel; // 导入

import java.util.ArrayList;
import java.util.List;

public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener,
        FilterDialog.OnFilterChangeListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private LinearLayout layoutEmpty;
    private LinearLayout btnFilter;

    private AlarmAdapter alarmAdapter;

    private List<AlarmItem> alarmList = new ArrayList<>();
    private List<AlarmItem> allAlarmsSource = new ArrayList<>();

    // 【新增 1】缓存设备列表，用于搜索时查找名称
    private List<DeviceItem> cachedDeviceList = new ArrayList<>();

    private String currentKeyword = "";
    private String filterType = "";
    private String filterLevel = "";
    private String filterStatus = "";
    private String filterLocation = "";

    private AlarmViewModel alarmViewModel;

    public AlarmFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(alarmList, this);
        recyclerView.setAdapter(alarmAdapter);

        // --- 初始化 DeviceViewModel ---
        DeviceViewModel deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        // 【修改 2】观察设备列表，保存到本地 cachedDeviceList，并传给 Adapter
        deviceViewModel.getLiveDeviceList().observe(getViewLifecycleOwner(), deviceList -> {
            // 1. 更新 Adapter 显示
            if (alarmAdapter != null) {
                alarmAdapter.setDeviceList(deviceList);
            }
            // 2. 更新本地缓存，用于搜索匹配
            cachedDeviceList.clear();
            if (deviceList != null) {
                cachedDeviceList.addAll(deviceList);
            }
            // 3. 设备名可能变了，重新跑一次筛选逻辑
            applyCombinedFilter();
        });

        // --- 初始化 AlarmViewModel ---
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        alarmViewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            allAlarmsSource.clear();
            allAlarmsSource.addAll(alarms);
            applyCombinedFilter();
        });

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { return false; }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentKeyword = newText;
                    applyCombinedFilter();
                    return true;
                }
            });
        }

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                FilterDialog dialog = FilterDialog.newInstance(filterType, filterLevel, filterStatus, filterLocation);
                dialog.setOnFilterChangeListener(this);
                dialog.show(getChildFragmentManager(), "filter");
            });
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof com.example.mineguard.MainActivity) {
                ((com.example.mineguard.MainActivity) getActivity()).manualRefreshAlarmConfig();
            }
            new android.os.Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        return view;
    }

    // ... onAlarmClick 等回调方法保持不变 ...
    @Override
    public void onAlarmClick(AlarmItem alarm) {
        AlarmDetailDialog dialog = AlarmDetailDialog.newInstance(alarm);
        dialog.setOnStatusChangeListener(this::onAlarmStatusChanged);
        dialog.show(getChildFragmentManager(), "detail");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) { }

    @Override
    public void onAlarmStatusChanged(AlarmItem alarm) {
        if (alarmViewModel != null) {
            alarmViewModel.update(alarm);
        }
    }

    @Override
    public void onFilterChanged(String alarmType, String alarmLevel, String status, String location) {
        this.filterType = alarmType;
        this.filterLevel = alarmLevel;
        this.filterStatus = status;
        this.filterLocation = location;
        applyCombinedFilter();
    }

    /**
     * 【核心修改 3】综合筛选逻辑：支持搜ID、设备名、区域
     */
    private void applyCombinedFilter() {
        alarmList.clear();

        for (AlarmItem item : allAlarmsSource) {
            boolean isMatch = true;

            // 1. 搜索关键词匹配
            if (currentKeyword != null && !currentKeyword.isEmpty()) {
                String k = currentKeyword.toLowerCase();

                // A. 基础匹配：ID、类型、原始IP
                boolean matchId = String.valueOf(item.getId()).contains(k);
                boolean matchType = item.getType() != null && item.getType().toLowerCase().contains(k);
                boolean matchRawIp = item.getIp() != null && item.getIp().toLowerCase().contains(k);

                // B. 【新增】高级匹配：查找关联的设备名和区域
                String deviceName = "";
                String deviceArea = "";

                // 遍历缓存的设备列表查找匹配项
                if (item.getIp() != null && !cachedDeviceList.isEmpty()) {
                    for (DeviceItem d : cachedDeviceList) {
                        if (d.getIpAddress() != null && d.getIpAddress().trim().equalsIgnoreCase(item.getIp().trim())) {
                            deviceName = d.getDeviceName();
                            deviceArea = d.getArea();
                            break;
                        }
                    }
                }

                boolean matchDeviceName = deviceName != null && deviceName.toLowerCase().contains(k);
                boolean matchDeviceArea = deviceArea != null && deviceArea.toLowerCase().contains(k);

                // 只要满足任意一项，就算匹配成功
                if (!matchId && !matchType && !matchRawIp && !matchDeviceName && !matchDeviceArea) {
                    isMatch = false;
                }
            }

            // 2. 筛选条件匹配 (保持不变)
            if (isMatch && !filterType.isEmpty()) {
                // 如果是"全部"已经在Dialog里处理成空字符串了，这里只处理非空
                // 如果筛选条件里有"异物"，而 item.getType() 也是"异物"，则通过
                if (item.getType() == null || !item.getType().contains(filterType)) isMatch = false;
            }

            if (isMatch && !filterLevel.isEmpty()) {
                if (!item.getLevelDescription().equals(filterLevel)) isMatch = false;
            }

            if (isMatch && !filterStatus.isEmpty()) {
                if (!item.getStatusDescription().equals(filterStatus)) isMatch = false;
            }

            if (isMatch && !filterLocation.isEmpty()) {
                // 这里不仅可以匹配 item.getLocation()，最好也匹配一下从 DeviceList 查出来的区域
                // 简单起见，这里暂且匹配原始 location，如果需要匹配设备配置区域，逻辑同上方的 deviceArea
                if (item.getLocation() == null || !item.getLocation().contains(filterLocation)) isMatch = false;
            }

            if (isMatch) {
                alarmList.add(item);
            }
        }

        if (alarmList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
        alarmAdapter.notifyDataSetChanged();
    }
}