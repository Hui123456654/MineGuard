package com.example.mineguard.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // 导入点击监听需要的
import android.widget.SearchView;   // 【修改】导入 SearchView
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
import java.util.ArrayList;
import java.util.List;

public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener,
        FilterDialog.OnFilterChangeListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    // 【修改 1】变量类型改为 SearchView
    private SearchView searchView;
    // 【新增】空状态布局
    private LinearLayout layoutEmpty;

    // 【新增】筛选按钮的容器 (为了绑定点击事件)
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

        // 【修改 2】绑定正确的 ID (对应你的 XML: android:id="@+id/searchView")
        searchView = view.findViewById(R.id.searchView);

        // 【新增】绑定筛选按钮 (对应 XML: android:id="@+id/btnFilter")
        btnFilter = view.findViewById(R.id.btnFilter);
        // 【新增】绑定空状态布局 (对应 XML: android:id="@+id/layoutEmpty")
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        // 2. 初始化列表
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(alarmList, this);
        recyclerView.setAdapter(alarmAdapter);

        // 3. 初始化 ViewModel
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // 4. 观察数据库变化
        alarmViewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            allAlarmsSource.clear();
            allAlarmsSource.addAll(alarms);
            // 每次数据更新，重新执行筛选
            applyCombinedFilter();
        });

        // 5. 【修改 3】SearchView 的专用监听写法
        if (searchView != null) {
            // 设置默认展开，避免点击放大镜才展开
            // searchView.setIconifiedByDefault(false); // XML里已经写了，这里可以不写

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // 点击软键盘搜索键时触发，这里不需要处理
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // 【关键】文字每变动一次，就触发筛选
                    currentKeyword = newText;
                    applyCombinedFilter();
                    return true;
                }
            });
        }

        // 6. 【新增】给 XML 里的筛选按钮添加点击事件
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                // 显示筛选弹窗，带入当前的筛选状态
                FilterDialog.newInstance(filterType, filterLevel, filterStatus, filterLocation)
                        .show(getChildFragmentManager(), "filter_dialog");

                // 记得设置监听器，不然弹窗选完没反应
                getChildFragmentManager().executePendingTransactions(); // 确保 Fragment 已经添加
            });
        }

        // 必须给 FilterDialog 设置监听器 (稍微特殊的处理，因为它是 DialogFragment)
        // 实际上 FilterDialog 应该在 onAttach 中或者 setListener 中绑定
        // 简单做法：我们让 FilterDialog 既然是 Fragment，就通过 FragmentResultListener 或者直接回调
        // 这里最简单的做法：修改 FilterDialog 的调用方式，见下文补充说明。
        // 但根据你之前的 FilterDialog 代码，我们需要在 show 之后手动 setListener
        // 由于 FilterDialog.newInstance 返回的是实例，我们可以在 show 之前处理
        // 下面的 btnFilter 点击事件稍微改写一下：

        btnFilter.setOnClickListener(v -> {
            FilterDialog dialog = FilterDialog.newInstance(filterType, filterLevel, filterStatus, filterLocation);
            dialog.setOnFilterChangeListener(this); // 【关键】把 Fragment 自己传进去作为监听器
            dialog.show(getChildFragmentManager(), "filter");
        });


        // 7. 下拉刷新逻辑
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

                // 现有逻辑：匹配 名称、IP、类型
                boolean matchName = item.getName() != null && item.getName().toLowerCase().contains(k);
                boolean matchIP = item.getIp() != null && item.getIp().contains(k);
                boolean matchType = item.getType() != null && item.getType().toLowerCase().contains(k);

                // 【新增】逻辑：匹配 ID (把数字转成字符串再搜)
                // 这样你搜 "5042" 或者 "50" 都能搜到了
                boolean matchId = String.valueOf(item.getId()).contains(k);

                // 【修改】判断条件：只要满足其中一项即可
                if (!matchName && !matchIP && !matchType && !matchId) {
                    isMatch = false;
                }
            }

            // 2. 筛选条件匹配 (FilterDialog) - 保持不变
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
        // 【核心新增逻辑】根据数据列表状态，控制视图显示
        if (alarmList.isEmpty()) {
            // 如果列表为空，显示提示，隐藏 RecyclerView
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            // 如果列表非空，显示 RecyclerView，隐藏提示
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
        alarmAdapter.notifyDataSetChanged();
    }
}