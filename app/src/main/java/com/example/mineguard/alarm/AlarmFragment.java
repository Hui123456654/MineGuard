package com.example.mineguard.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // 必须导入
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.mineguard.R;
import com.example.mineguard.alarm.adapter.AlarmAdapter;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.alarm.dialog.AlarmDetailDialog;
import com.example.mineguard.alarm.dialog.FilterDialog;
import java.util.ArrayList;
import java.util.List;

public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener,
        FilterDialog.OnFilterChangeListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> alarmList = new ArrayList<>();

    // 1. 【新增】ViewModel
    private AlarmViewModel alarmViewModel;

    public AlarmFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(alarmList, this);
        recyclerView.setAdapter(alarmAdapter);

        // 2. 【新增】初始化并观察数据
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // 当数据库变动时，这里自动执行
        alarmViewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            alarmList.clear();
            alarmList.addAll(alarms);
            alarmAdapter.notifyDataSetChanged();
        });

        // 3. 【修改】下拉刷新不需要拉数据了，只需要刷新相机配置
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof com.example.mineguard.MainActivity) {
                ((com.example.mineguard.MainActivity) getActivity()).manualRefreshAlarmConfig();
            }
            // 延迟1秒关闭刷新动画
            new android.os.Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        return view;
    }

    // 【注意】原来的 onResume, onPause, onNewAlarm 方法全部删除了
    // 因为现在是 ViewModel 自动管理数据，不需要手动监听 Activity 了

    @Override
    public void onAlarmClick(AlarmItem alarm) {
        AlarmDetailDialog.newInstance(alarm).show(getChildFragmentManager(), "detail");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) { }

    // 3. 【新增】实现我们在 Adapter 里新定义的接口方法
    @Override
    public void onAlarmStatusChanged(AlarmItem alarm) {
        // 这里收到了 Adapter 传来的已经修改过状态的 alarm 对象
        // 调用 ViewModel 更新数据库
        if (alarmViewModel != null) {
            alarmViewModel.update(alarm);
        }
    }
    @Override
    public void onFilterChanged(String t, String l, String s, String loc) { }
}