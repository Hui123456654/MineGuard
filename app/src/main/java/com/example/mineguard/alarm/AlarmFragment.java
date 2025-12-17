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
import com.example.mineguard.data.DeviceViewModel; // 1. 新增导入：设备数据 ViewModel

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

    // 数据列表
    private List<AlarmItem> alarmList = new ArrayList<>();
    private List<AlarmItem> allAlarmsSource = new ArrayList<>(); // 源数据备份

    // 筛选条件缓存
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

        // 1. 绑定控件
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        // 2. 初始化列表
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(alarmList, this);
        recyclerView.setAdapter(alarmAdapter);

        // ================== 【新增代码开始】 ==================
        // 3. 初始化 DeviceViewModel (获取系统配置里的设备列表)
        // 关键点：使用 requireActivity() 范围，这样才能拿到ConfigurationFragment里保存的数据
        DeviceViewModel deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        // 观察设备列表变化，一旦变化（或初始化），就传给 Adapter
        deviceViewModel.getLiveDeviceList().observe(getViewLifecycleOwner(), deviceList -> {
            if (alarmAdapter != null) {
                // 调用我们在 Adapter 里刚写好的 setDeviceList 方法
                alarmAdapter.setDeviceList(deviceList);
            }
        });
        // ================== 【新增代码结束】 ==================

        // 4. 初始化 AlarmViewModel (数据库里的报警数据)
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // 5. 观察数据库变化
        alarmViewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            allAlarmsSource.clear();
            allAlarmsSource.addAll(alarms);
            // 每次数据更新，重新执行筛选
            applyCombinedFilter();
        });

        // 6. SearchView 的专用监听写法
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentKeyword = newText;
                    applyCombinedFilter();
                    return true;
                }
            });
        }

        // 7. 筛选按钮点击事件
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                FilterDialog dialog = FilterDialog.newInstance(filterType, filterLevel, filterStatus, filterLocation);
                dialog.setOnFilterChangeListener(this); // 把 Fragment 自己传进去作为监听器
                dialog.show(getChildFragmentManager(), "filter");
            });
        }

        // 8. 下拉刷新逻辑
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof com.example.mineguard.MainActivity) {
                ((com.example.mineguard.MainActivity) getActivity()).manualRefreshAlarmConfig();
            }
            new android.os.Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        return view;
    }

    // ================== 事件回调 ==================

    @Override
    public void onAlarmClick(AlarmItem alarm) {
        AlarmDetailDialog.newInstance(alarm).show(getChildFragmentManager(), "detail");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) { }

    @Override
    public void onAlarmStatusChanged(AlarmItem alarm) {
        if (alarmViewModel != null) {
            alarmViewModel.update(alarm);
        }
    }

    // ================== 筛选逻辑 ==================

    @Override
    public void onFilterChanged(String alarmType, String alarmLevel, String status, String location) {
        this.filterType = alarmType;
        this.filterLevel = alarmLevel;
        this.filterStatus = status;
        this.filterLocation = location;
        applyCombinedFilter();
    }

    /**
     * 综合筛选逻辑
     */
    private void applyCombinedFilter() {
        alarmList.clear();

        for (AlarmItem item : allAlarmsSource) {
            boolean isMatch = true;

            // 1. 搜索关键词匹配
            if (currentKeyword != null && !currentKeyword.isEmpty()) {
                String k = currentKeyword.toLowerCase();

                // 匹配 名称、IP、类型
                boolean matchName = item.getName() != null && item.getName().toLowerCase().contains(k);
                boolean matchIP = item.getIp() != null && item.getIp().contains(k);
                boolean matchType = item.getType() != null && item.getType().toLowerCase().contains(k);
                // 匹配 ID
                boolean matchId = String.valueOf(item.getId()).contains(k);

                if (!matchName && !matchIP && !matchType && !matchId) {
                    isMatch = false;
                }
            }

            // 2. 筛选条件匹配
            if (isMatch && !filterType.isEmpty()) {
                if (item.getType() == null || !item.getType().equals(filterType)) isMatch = false;
            }

            if (isMatch && !filterLevel.isEmpty()) {
                if (!item.getLevelDescription().equals(filterLevel)) isMatch = false;
            }

            if (isMatch && !filterStatus.isEmpty()) {
                if (!item.getStatusDescription().equals(filterStatus)) isMatch = false;
            }

            if (isMatch && !filterLocation.isEmpty()) {
                if (item.getLocation() == null || !item.getLocation().contains(filterLocation)) isMatch = false;
            }

            if (isMatch) {
                alarmList.add(item);
            }
        }

        // 视图状态控制
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