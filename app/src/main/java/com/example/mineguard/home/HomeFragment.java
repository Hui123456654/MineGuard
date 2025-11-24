package com.example.mineguard.home;
import static com.example.mineguard.MyApplication.globalIP;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.mineguard.R;
import com.example.mineguard.data.StatisticsData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.Callback;
import java.io.IOException;
import okhttp3.Call;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvTotalAlarms;
    private TextView tvUnProcessedAlarms;
    private TextView tvTotalDevices;
    private TextView tvOnlineDevices;

    private LineChart chartAlarmTrends;
    private PieChart chartTypeStatistics;

    private TextView tvTimeOneWeek;
    private TextView tvTimeOneYear;

    private TextView tvTypeChartTitle;

    private enum TimeSpan {
        ONE_WEEK, ONE_MONTH, ONE_YEAR
    }
    //今日概览成员变量
    private JSONObject platformStats; // 存储平台统计数据

    //报警趋势图成员变量
    private TimeSpan currentTimeSpan = TimeSpan.ONE_WEEK;
    private JSONObject apiDailyTotal; // 存储每日报警统计
    private JSONObject apiMonthlyTotal; // 存储每月报警统计



    // 排行榜成员变量
    private HorizontalBarChart chartAlarmRanking;  // 报警类型排行榜图表
    private TextView tvRankingTimeOneWeek;  // 排行榜周切换按钮
    private TextView tvRankingTimeOneMonth;  // 排行榜月切换按钮
    private JSONObject apiWeekTop;  // 存储周排行数据
    private JSONObject apiMonthTop;  // 存储月排行数据
    private TimeSpan currentRankingTimeSpan = TimeSpan.ONE_WEEK;  // 当前排行榜时间跨度

    //周/月报警类型统计成员变量
    private JSONObject apiWeekTotal; // 存储周报警类型统计数据
    private JSONObject apiMonthTotal; // 存储月报警类型统计数据
    private PieChart chartWeekMonthalarmType; // 新的饼图(周/月报警类型统计)
    private TextView tvWeekMonthAlarmTypeTitle; // 新饼图标题
    private TextView tvWeekMonthAlarmTypeTimeOneWeek; // 新饼图周切换按钮
    private TextView tvWeekMonthAlarmTypeTimeOneMonth; // 新饼图月切换按钮
    private TimeSpan currentWeekMonthAlarmTypeTimeSpan = TimeSpan.ONE_WEEK; // 当前新饼图时间跨度
    
    //处理报警数据成员变量
    private JSONObject processingInfo; // 存储处理中报警数据

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupTimeSpanListeners();
        fetchStatisticsDataFromApi();
        return view;
    }

    private void initViews(View view) {
        // Overview card views
        tvTotalAlarms = view.findViewById(R.id.tv_total_alarms);
        tvUnProcessedAlarms = view.findViewById(R.id.tv_unprocessed_alarms);
        tvTotalDevices = view.findViewById(R.id.tv_total_devices);
        tvOnlineDevices = view.findViewById(R.id.tv_online_devices);

        // Chart views
        chartAlarmTrends = view.findViewById(R.id.chart_alarm_trends);
        //chartDeviceStatistics = view.findViewById(R.id.card_device_statistics).findViewById(R.id.pie_chart);
        chartTypeStatistics = view.findViewById(R.id.card_type_statistics).findViewById(R.id.pie_chart);

        // Time span views
        tvTimeOneWeek = view.findViewById(R.id.tv_time_one_week);
        tvTimeOneYear = view.findViewById(R.id.tv_time_one_year);

        // 新增初始化代码
        chartAlarmRanking = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.bar_chart);
        tvRankingTimeOneWeek = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_week);
        tvRankingTimeOneMonth = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_month);

        // 初始化新饼图相关视图
        chartWeekMonthalarmType = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.pie_chart);
        tvWeekMonthAlarmTypeTitle = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_chart_title);
        tvWeekMonthAlarmTypeTimeOneWeek = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_week);
        tvWeekMonthAlarmTypeTimeOneMonth = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_month);

        // Chart title views
        TextView tvBarChartTitle = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_chart_title);
        tvWeekMonthAlarmTypeTitle.setText(R.string.week_month_alarm_type_statistics);
        tvTypeChartTitle = view.findViewById(R.id.card_type_statistics).findViewById(R.id.tv_chart_title);

        // Set chart titles
        tvBarChartTitle.setText(R.string.alarm_ranking); // 设置正确的标题
        tvTypeChartTitle.setText(R.string.type_statistics);
    }

    private void setupTimeSpanListeners() {
        tvTimeOneWeek.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_WEEK));
        tvTimeOneYear.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_YEAR));

        // 新增排行榜时间切换监听器
        tvRankingTimeOneWeek.setOnClickListener(v -> selectRankingTimeSpan(TimeSpan.ONE_WEEK));
        tvRankingTimeOneMonth.setOnClickListener(v -> selectRankingTimeSpan(TimeSpan.ONE_MONTH));

        // 添加新饼图时间切换监听器
        tvWeekMonthAlarmTypeTimeOneWeek.setOnClickListener(v -> selectWeekMonthAlarmTypeTimeSpan(TimeSpan.ONE_WEEK));
        tvWeekMonthAlarmTypeTimeOneMonth.setOnClickListener(v -> selectWeekMonthAlarmTypeTimeSpan(TimeSpan.ONE_MONTH));
    }

    private void selectTimeSpan(TimeSpan timeSpan) {
        currentTimeSpan = timeSpan;

        // Reset all time span text views
        resetTimeSpanStyles();

        // Set selected style
        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_WEEK:
                selectedView = tvTimeOneWeek;
                break;
            case ONE_YEAR:
                selectedView = tvTimeOneYear;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }

        // Reload chart data with new time span
        loadAlarmTrendsData();
    }

    private void resetTimeSpanStyles() {
        TextView[] timeSpanViews = {tvTimeOneWeek, tvTimeOneYear};
        for (TextView view : timeSpanViews) {
            view.setTextColor(getResources().getColor(R.color.text_secondary));
            view.setBackground(null);
        }
    }

    private void selectRankingTimeSpan(TimeSpan timeSpan) {
        currentRankingTimeSpan = timeSpan;

        // 重置所有时间跨度文本视图样式
        resetRankingTimeSpanStyles();

        // 设置选中样式
        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_WEEK:
                selectedView = tvRankingTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvRankingTimeOneMonth;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }

        // 重新加载排行榜数据
        loadAlarmRankingData();
    }

    private void resetRankingTimeSpanStyles() {
        TextView[] timeSpanViews = {tvRankingTimeOneWeek, tvRankingTimeOneMonth};
        for (TextView view : timeSpanViews) {
            if (view != null) {
                view.setTextColor(getResources().getColor(R.color.text_secondary));
                view.setBackground(null);
            }
        }
    }

    private void selectWeekMonthAlarmTypeTimeSpan(TimeSpan timeSpan) {
        currentWeekMonthAlarmTypeTimeSpan = timeSpan;

        // 重置所有时间跨度文本视图样式
        resetWeekMonthAlarmTypeTimeSpanStyles();

        // 设置选中样式
        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_WEEK:
                selectedView = tvWeekMonthAlarmTypeTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvWeekMonthAlarmTypeTimeOneMonth;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }

        // 重新加载新饼图数据
        loadWeekMonthAlarmTypeData();
    }

    private void resetWeekMonthAlarmTypeTimeSpanStyles() {
        TextView[] timeSpanViews = {tvWeekMonthAlarmTypeTimeOneWeek, tvWeekMonthAlarmTypeTimeOneMonth};
        for (TextView view : timeSpanViews) {
            if (view != null) {
                view.setTextColor(getResources().getColor(R.color.text_secondary));
                view.setBackground(null);
            }
        }
    }


    private void fetchStatisticsDataFromApi() {
        Log.d("HomeFragment", "开始API请求...");
        // 使用GET请求
        String url = "http://" + globalIP + ":5004/data/mobile_summary?page=1&limitNum=20";
        // 创建OkHttp客户端和GET请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get() // 明确指定GET方法
                .build();
        // 异步发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("HomeFragment", "API请求失败: " + e.getMessage());
                Log.e("HomeFragment", "请求URL: " + url);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "网络请求失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("HomeFragment", "收到API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d("HomeFragment", "响应数据: " + jsonData);
                    try {
                        // 解析JSON数据
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getInt("code") == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            // 提取平台统计数据
                            if (data.has("platform_stats")) {
                                platformStats = data.getJSONObject("platform_stats");
                            }
                            // 提取趋势图数据
                            if (data.has("daily_total")) {
                                apiDailyTotal = data.getJSONObject("daily_total");
                            }
                            if (data.has("monthly_total")) {
                                apiMonthlyTotal = data.getJSONObject("monthly_total");
                            }
                            // 提取排行榜数据
                            if (data.has("week_top")) {
                                apiWeekTop = data.getJSONObject("week_top");
                            }
                            if (data.has("month_top")) {
                                apiMonthTop = data.getJSONObject("month_top");
                            }
                            //提取本周/本月报警类型统计
                            if (data.has("week_total")) {
                                apiWeekTotal = data.getJSONObject("week_total");
                            }
                            if (data.has("month_total")) {
                                apiMonthTotal = data.getJSONObject("month_total");
                            }
                            // 提取报警数据
                            if (data.has("processing_info")) {
                                processingInfo = data.getJSONObject("processing_info");
                                Log.d("HomeFragment", "成功获取processing_info数据");
                            }

                            // 在主线程更新UI
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // 更新平台统计数据
                                loadPlatformStatisticsData();
                                // 更新趋势图
                                loadAlarmTrendsData();
                                // 更新排行榜数据
                                loadAlarmRankingData();
                                // 更新周/月报警类型饼图数据
                                loadWeekMonthAlarmTypeData();
                                // 更新报警类型统计图
                                loadAlarmTypeStatisticsData();
                            });
                        } else {
                            Log.e("HomeFragment", "API返回错误码: " + jsonObject.getInt("code"));
                        }
                    } catch (JSONException e) {
                        Log.e("HomeFragment", "JSON解析错误: " + e.getMessage());
                    }
                } else {
                    Log.e("HomeFragment", "API响应失败: " + response.code());
                }
            }
        });
    }

    private void loadPlatformStatisticsData() {
        if (platformStats != null) {
            try {
                // 提取所需数据
                int totalAlerts = platformStats.getInt("total_alerts");
                int unprocessedAlerts = platformStats.getInt("unresolved_alerts");
                int cameraCount = platformStats.getInt("camera_count");

                // 计算在线设备数（余煤、挂钩分割版、旋转器的count之和）
                int onlineDevices = 0;
                if (platformStats.has("余煤")) {
                    onlineDevices += platformStats.getJSONObject("余煤").getInt("count");
                }
                if (platformStats.has("挂钩分割版")) {
                    onlineDevices += platformStats.getJSONObject("挂钩分割版").getInt("count");
                }
                if (platformStats.has("旋转器")) {
                    onlineDevices += platformStats.getJSONObject("旋转器").getInt("count");
                }

                tvTotalAlarms.setText(String.valueOf(totalAlerts));
                tvUnProcessedAlarms.setText(String.valueOf(unprocessedAlerts));
                tvTotalDevices.setText(String.valueOf(cameraCount));
                tvOnlineDevices.setText(String.valueOf(onlineDevices));
            } catch (JSONException e) {
                Log.e("HomeFragment", "解析平台统计数据错误: " + e.getMessage());
                // 发生错误时直接使用模拟数据
                setMockStatisticsUI();
            }
        } else {
            // 发生错误时直接使用模拟数据
            setMockStatisticsUI();
        }
    }

    private void setMockStatisticsUI() {
        Log.d("HomeFragment", "使用模拟统计数据");
        tvTotalAlarms.setText("6");
        tvUnProcessedAlarms.setText("2");
        tvTotalDevices.setText("6");
        tvOnlineDevices.setText("6");
    }

    // 生成报警趋势数据:根据API返回的daily_total/monthly_total数据
    private void loadAlarmTrendsData() {
        List<StatisticsData.AlarmTrendData> trendData;

        // 尝试使用API数据
        if (currentTimeSpan == TimeSpan.ONE_WEEK && apiDailyTotal != null) {
            trendData = generateApiWeeklyData(apiDailyTotal);
        } else if (currentTimeSpan == TimeSpan.ONE_YEAR && apiMonthlyTotal != null) {
            trendData = generateApiYearlyData(apiMonthlyTotal);
        } else {
            // API数据不可用时直接在方法内部生成模拟数据
            trendData = new ArrayList<>();
            Random random = new Random();

            switch (currentTimeSpan) {
                case ONE_WEEK:
                    String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    for (String day : days) {
                        trendData.add(new StatisticsData.AlarmTrendData(day, random.nextInt(50) + 10));
                    }
                    break;
                case ONE_YEAR:
                    String[] months = {"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
                    for (String month : months) {
                        trendData.add(new StatisticsData.AlarmTrendData(month, random.nextInt(100) + 20));
                    }
                    break;
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < trendData.size(); i++) {
            entries.add(new Entry(i, trendData.get(i).getAlarmCount()));
            labels.add(trendData.get(i).getTimeLabel());
        }

        LineDataSet dataSet = new LineDataSet(entries, "报警数量");
        dataSet.setColor(getResources().getColor(R.color.primary_blue));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_blue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // 添加整数值格式化器
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        chartAlarmTrends.setData(lineData);

        // Customize chart appearance
        chartAlarmTrends.getDescription().setEnabled(false);
        chartAlarmTrends.setTouchEnabled(true);
        chartAlarmTrends.setDragEnabled(true);
        chartAlarmTrends.setScaleEnabled(true);
        chartAlarmTrends.setPinchZoom(true);
        chartAlarmTrends.setDrawGridBackground(false);
        chartAlarmTrends.getLegend().setEnabled(false);

        // Customize x-axis
        chartAlarmTrends.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        // 设置Y轴最小值为0，确保不会显示负数
        chartAlarmTrends.getAxisLeft().setAxisMinimum(0f);
        chartAlarmTrends.getAxisRight().setAxisMinimum(0f);
        chartAlarmTrends.invalidate();
    }

    // 添加处理API每周数据的方法
    private List<StatisticsData.AlarmTrendData> generateApiWeeklyData(JSONObject dailyTotal) {
        List<StatisticsData.AlarmTrendData> data = new ArrayList<>();

        try {
            // 获取日期列表
            JSONArray dateList = dailyTotal.getJSONArray("dateList");
            // 获取各类型报警数量
            JSONObject numLists = dailyTotal.getJSONObject("numLists");

            // 对每一天计算总报警数量
            for (int i = 0; i < dateList.length(); i++) {
                String date = dateList.getString(i);
                int totalCount = 0;

                // 遍历所有报警类型，累加数量
                JSONArray keys = numLists.names();
                if (keys != null) {
                    for (int j = 0; j < keys.length(); j++) {
                        String type = keys.getString(j);
                        JSONArray counts = numLists.getJSONArray(type);
                        if (i < counts.length()) {
                            totalCount += counts.getInt(i);
                        }
                    }
                }

                // 添加到数据列表
                data.add(new StatisticsData.AlarmTrendData(date, totalCount));
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析每周数据错误: " + e.getMessage());
        }

        return data;
    }

    // 添加处理API年度数据的方法
    private List<StatisticsData.AlarmTrendData> generateApiYearlyData(JSONObject monthlyTotal) {
        List<StatisticsData.AlarmTrendData> data = new ArrayList<>();

        try {
            // 获取月份列表
            JSONArray monthList = monthlyTotal.getJSONArray("month");
            // 获取每月报警总数
            JSONArray alarmNumArray = monthlyTotal.getJSONArray("alarmNum");

            // 处理每月数据
            for (int i = 0; i < monthList.length() && i < alarmNumArray.length(); i++) {
                String month = monthList.getString(i);
                // 从月份字符串中提取月数字，如"2025-01" -> "1月"
                String monthLabel = month.substring(5) + "月";
                int alarmCount = alarmNumArray.getInt(i);

                // 添加到数据列表
                data.add(new StatisticsData.AlarmTrendData(monthLabel, alarmCount));
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析年度数据错误: " + e.getMessage());
        }

        return data;
    }


    // 生成报警排行榜数据:根据API返回的week_top、month_top数据
    private void loadAlarmRankingData() {
        List<AlarmRankingData> rankingData;

        // 尝试使用API数据
        if (currentRankingTimeSpan == TimeSpan.ONE_WEEK && apiWeekTop != null) {
            rankingData = generateApiRankingData(apiWeekTop);
        } else if (currentRankingTimeSpan == TimeSpan.ONE_MONTH && apiMonthTop != null) {
            rankingData = generateApiRankingData(apiMonthTop);
        } else {
            // API数据不可用时使用模拟数据
            rankingData = new ArrayList<>();
            rankingData.add(new AlarmRankingData("余煤检测", 2));
            rankingData.add(new AlarmRankingData("旋转器检测", 5));
            rankingData.add(new AlarmRankingData("挂钩检测分割版", 3));
        }

        // 创建柱状图数据
        ArrayList<BarEntry> entries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();

        // 为柱状图准备数据
        for (int i = 0; i < rankingData.size(); i++) {
            entries.add(new BarEntry(i, rankingData.get(i).getValue()));
            labels.add(rankingData.get(i).getName());
        }

        // 不设置图例标签
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(getResources().getColor(R.color.primary_blue));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true);

        // 添加整数值格式化器
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        // 设置柱宽
        barData.setBarWidth(0.6f);
        chartAlarmRanking.setData(barData);

        // 自定义图表外观
        chartAlarmRanking.getDescription().setEnabled(false);
        chartAlarmRanking.setTouchEnabled(true);
        chartAlarmRanking.setDragEnabled(true);
        chartAlarmRanking.setScaleEnabled(true);
        chartAlarmRanking.setDrawGridBackground(false);
        chartAlarmRanking.setDrawBarShadow(false);
        chartAlarmRanking.setDrawValueAboveBar(true);

        // 禁用图例
        chartAlarmRanking.getLegend().setEnabled(false);
        // 调整图表间距，确保底部和左侧有足够空间显示标签
        chartAlarmRanking.setExtraOffsets(40, 10, 20, 60);

        // 配置X轴显示报警类型名称（在底部）
        XAxis xAxis = chartAlarmRanking.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(rankingData.size() - 0.5f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-30f); // 旋转标签以避免重叠
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) Math.round(value);
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        // 配置Y轴
        YAxis leftAxis = chartAlarmRanking.getAxisLeft();
        leftAxis.setEnabled(true);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setTextSize(12f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // 设置Y轴最大值
        float maxValue = 0;
        for (BarEntry entry : entries) {
            if (entry.getY() > maxValue) {
                maxValue = entry.getY();
            }
        }
        // 添加10%的余量
        leftAxis.setAxisMaximum(maxValue * 1.1f);

        // 禁用右侧Y轴
        chartAlarmRanking.getAxisRight().setEnabled(false);

        chartAlarmRanking.invalidate();
    }

    // 从API数据生成排行榜数据
    private List<AlarmRankingData> generateApiRankingData(JSONObject rankingData) {
        List<AlarmRankingData> data = new ArrayList<>();

        try {
            if (rankingData.has("alerts")) {
                JSONArray alertsArray = rankingData.getJSONArray("alerts");
                for (int i = 0; i < alertsArray.length(); i++) {
                    JSONObject alertObj = alertsArray.getJSONObject(i);
                    if (alertObj.has("name") && alertObj.has("value")) {
                        String name = alertObj.getString("name");
                        int value = alertObj.getInt("value");
                        data.add(new AlarmRankingData(name, value));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析排行榜数据错误: " + e.getMessage());
        }

        return data;
    }

    // 排行榜数据类
    private static class AlarmRankingData {
        private String name;
        private int value;

        public AlarmRankingData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    private void loadWeekMonthAlarmTypeData() {
        List<StatisticsData.AlarmTypeData> typeData;

        // 尝试使用API数据
        if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK && apiWeekTotal != null) {
            typeData = generateAlarmTypeDataFromWeekMonthTotal(apiWeekTotal);
            Log.d("HomeFragment", "使用API周数据生成报警类型统计");
        } else if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_MONTH && apiMonthTotal != null) {
            typeData = generateAlarmTypeDataFromWeekMonthTotal(apiMonthTotal);
            Log.d("HomeFragment", "使用API月数据生成报警类型统计");
        } else {
            // API数据不可用时使用模拟数据
            typeData = new ArrayList<>();
            if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK) {
                // 模拟周数据
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 3));
                typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 5));
                typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 2));
            } else {
                // 模拟月数据
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 12));
                typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 18));
                typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 8));
            }
            Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
        }

        // 确保有数据可显示
        if (typeData.isEmpty()) {
            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
            Log.d("HomeFragment", "添加报警类型: " + data.getTypeName() + ", 数量: " + data.getAlarmCount());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        // 添加整数值格式化器
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        PieData pieData = new PieData(dataSet);
        chartWeekMonthalarmType.setData(pieData);

        // 自定义饼图外观
        chartWeekMonthalarmType.getDescription().setEnabled(false);
        chartWeekMonthalarmType.setDrawHoleEnabled(true);
        chartWeekMonthalarmType.setHoleColor(Color.WHITE);
        chartWeekMonthalarmType.setTransparentCircleRadius(61f);
        chartWeekMonthalarmType.setHoleRadius(58f);
        chartWeekMonthalarmType.setDrawCenterText(true);
        chartWeekMonthalarmType.setCenterText(currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK ? "本周报警分布" : "本月报警分布");
        chartWeekMonthalarmType.setCenterTextSize(16f);
        chartWeekMonthalarmType.getLegend().setEnabled(true);
        chartWeekMonthalarmType.getLegend().setTextSize(12f);

        chartWeekMonthalarmType.invalidate();
    }

    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromWeekMonthTotal(JSONObject weekMonthTotal) {
        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();

        try {
            // 获取报警类型列表
            if (weekMonthTotal.has("alert_types")) {
                JSONArray alertTypes = weekMonthTotal.getJSONArray("alert_types");
                // 获取各类型数量映射
                if (weekMonthTotal.has("counts")) {
                    JSONObject counts = weekMonthTotal.getJSONObject("counts");

                    // 遍历所有报警类型
                    for (int i = 0; i < alertTypes.length(); i++) {
                        String type = alertTypes.getString(i);
                        if (counts.has(type)) {
                            int count = counts.getInt(type);
                            typeData.add(new StatisticsData.AlarmTypeData(type, count));
                            Log.d("HomeFragment", "添加统计结果: " + type + " = " + count);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析week_total/month_total数据失败: " + e.getMessage());
        }

        return typeData;
    }

    // 生成报警类型统计数据:根据API返回的processing_info数据
    private void loadAlarmTypeStatisticsData() {
        List<StatisticsData.AlarmTypeData> typeData;

        // 尝试使用API返回的processing_info数据
        if (processingInfo != null) {
            typeData = generateAlarmTypeDataFromApi(processingInfo);
            Log.d("HomeFragment", "使用API数据生成报警类型统计，类型数量: " + typeData.size());
        } else {
            // API数据不可用时直接在方法内生成模拟数据，不再调用独立方法
            typeData = new ArrayList<>();
            Random random = new Random();
            typeData.add(new StatisticsData.AlarmTypeData("入侵报警", random.nextInt(25) + 10));
            typeData.add(new StatisticsData.AlarmTypeData("火灾报警", random.nextInt(20) + 8));
            typeData.add(new StatisticsData.AlarmTypeData("设备故障", random.nextInt(30) + 12));
            typeData.add(new StatisticsData.AlarmTypeData("环境异常", random.nextInt(15) + 5));
            typeData.add(new StatisticsData.AlarmTypeData("其他报警", random.nextInt(18) + 7));
            Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
        }

        // 确保有数据可显示
        if (typeData.isEmpty()) {
            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
            Log.d("HomeFragment", "添加报警类型: " + data.getTypeName() + ", 数量: " + data.getAlarmCount());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        // 添加整数值格式化器
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        PieData pieData = new PieData(dataSet);
        chartTypeStatistics.setData(pieData);

        // Customize chart appearance
        chartTypeStatistics.getDescription().setEnabled(false);
        chartTypeStatistics.setDrawHoleEnabled(true);
        chartTypeStatistics.setHoleColor(Color.WHITE);
        chartTypeStatistics.setTransparentCircleRadius(61f);
        chartTypeStatistics.setHoleRadius(58f);
        chartTypeStatistics.setDrawCenterText(true);
        chartTypeStatistics.setCenterText("类型分布");
        chartTypeStatistics.setCenterTextSize(16f);
        chartTypeStatistics.getLegend().setEnabled(true);
        chartTypeStatistics.getLegend().setTextSize(12f);

        chartTypeStatistics.invalidate();
    }

    // 生成报警类型统计数据:根据API返回的processing_info数据
    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromApi(JSONObject processingInfo) {
        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();
        Map<String, Integer> typeCountMap = new HashMap<>();

        try {
            // 检查processing_info是否包含list字段
            if (processingInfo.has("list")) {
                JSONArray alarmList = processingInfo.getJSONArray("list");
                Log.d("HomeFragment", "processing_info.list长度: " + alarmList.length());

                // 统计每种报警类型的数量
                for (int i = 0; i < alarmList.length(); i++) {
                    JSONObject alarmItem = alarmList.getJSONObject(i);
                    if (alarmItem.has("type")) {
                        String type = alarmItem.getString("type");
                        typeCountMap.put(type, typeCountMap.getOrDefault(type, 0) + 1);
                        Log.d("HomeFragment", "处理报警项 " + i + ": type=" + type);
                    }
                }

                // 将统计结果转换为AlarmTypeData列表
                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                    typeData.add(new StatisticsData.AlarmTypeData(entry.getKey(), entry.getValue()));
                    Log.d("HomeFragment", "添加统计结果: " + entry.getKey() + " = " + entry.getValue());
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析processing_info数据失败: " + e.getMessage());
        }

        return typeData;
    }

    private int[] getColorArray() {
        return new int[]{
                getResources().getColor(R.color.primary_blue),
                getResources().getColor(R.color.primary_green),
                getResources().getColor(R.color.primary_orange),
                getResources().getColor(R.color.primary_red),
                getResources().getColor(R.color.primary_purple)
        };
    }
}