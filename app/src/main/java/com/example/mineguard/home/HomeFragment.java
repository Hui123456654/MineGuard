package com.example.mineguard.home;
import static com.example.mineguard.MyApplication.globalIP;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
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
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import java.util.Collections;

public class HomeFragment extends Fragment {

    //今日概览成员变量
    private JSONObject platformStats; // 存储平台统计数据
    private TextView tvProcessedPercent;
    private ProgressBar pbRate;
    private TextView tvTotalAlarmsHeader;
    private TextView tvUntreatedAlarmsHeader;
    private TextView tvTotalDevicesHeader;
    private TextView tvOnlineDevicesHeader;

    private LineChart chartAlarmTrends;
    //private PieChart chartTypeStatistics;

    private TextView tvTimeOneWeek;
    private TextView tvTimeOneYear;

    private TextView tvTypeChartTitle;

    private enum TimeSpan {
        ONE_WEEK, ONE_MONTH, ONE_YEAR
    }

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

    public static class AlarmRankingData {
        String name;
        int value;
        public AlarmRankingData(String name, int value) { this.name = name; this.value = value; }
        public String getName() { return name; }
        public int getValue() { return value; }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. 绑定新UI控件
        initHeaderViews(view);
        initViews(view);
        setupTimeSpanListeners();
        fetchStatisticsDataFromApi();
        return view;
    }

    private void initHeaderViews(View root) {
        View overviewCard = root.findViewById(R.id.card_header_overview);
        tvProcessedPercent = overviewCard.findViewById(R.id.tv_processed_percent);
        pbRate = root.findViewById(R.id.pb_overview_rate);

        // 绑定报警卡片内的控件
        View alarmCard = root.findViewById(R.id.card_header_alarm);
        tvTotalAlarmsHeader = alarmCard.findViewById(R.id.tv_total_alarms_header);
        tvUntreatedAlarmsHeader = alarmCard.findViewById(R.id.tv_untreated_alarms_header);

        // 绑定设备卡片内的控件
        View deviceCard = root.findViewById(R.id.card_header_device);
        tvTotalDevicesHeader = deviceCard.findViewById(R.id.tv_total_devices_header);
        tvOnlineDevicesHeader = deviceCard.findViewById(R.id.tv_online_devices_header);
    }

    private void initViews(View view) {
        // Chart views
        chartAlarmTrends = view.findViewById(R.id.chart_alarm_trends);
        //chartTypeStatistics = view.findViewById(R.id.card_type_statistics).findViewById(R.id.pie_chart);

        // Time span views
        tvTimeOneWeek = view.findViewById(R.id.tv_time_one_week);
        tvTimeOneYear = view.findViewById(R.id.tv_time_one_year);

        // 新增初始化代码
        chartAlarmRanking = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.bar_chart);
        tvRankingTimeOneWeek = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_week);
        tvRankingTimeOneMonth = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_month);
        setupHorizontalChart();

        // 初始化新饼图相关视图
        chartWeekMonthalarmType = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.pie_chart);
        tvWeekMonthAlarmTypeTitle = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_chart_title);
        tvWeekMonthAlarmTypeTimeOneWeek = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_week);
        tvWeekMonthAlarmTypeTimeOneMonth = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_month);

        // Chart title views
        TextView tvBarChartTitle = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_chart_title);
        //tvWeekMonthAlarmTypeTitle.setText(R.string.week_month_alarm_type_statistics);
        //tvTypeChartTitle = view.findViewById(R.id.card_type_statistics).findViewById(R.id.tv_chart_title);

        // Set chart titles
        tvBarChartTitle.setText(R.string.alarm_ranking); // 设置正确的标题
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
    private void updateHeaderUI(int totalAlarms, int unprocessedAlarms, int totalDevices, int onlineDevices) {
        // --- 1. 更新今日概览卡片 ---
        // 避免除以0错误
        int processedAlarms = totalAlarms - unprocessedAlarms;
        int percent = 0;
        if (totalAlarms > 0) {
            percent = (int) (((float) processedAlarms / totalAlarms) * 100);
        }

        // 设置百分比 (例如: 85%)
        tvProcessedPercent.setText(percent + "%");
        pbRate.setProgress(percent);

        // --- 2. 更新报警卡片  ---
        tvTotalAlarmsHeader.setText(String.valueOf(totalAlarms));
        tvUntreatedAlarmsHeader.setText(String.valueOf(unprocessedAlarms));

        // --- 3. 更新设备卡片 ---
        tvTotalDevicesHeader.setText(String.valueOf(totalDevices));
        tvOnlineDevicesHeader.setText(String.valueOf(onlineDevices));
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
                                //loadAlarmTypeStatisticsData();
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
//        if (platformStats != null) {
//            try {
//                // 提取所需数据
//                int totalAlerts = platformStats.getInt("total_alerts");
//                int unprocessedAlerts = platformStats.getInt("unresolved_alerts");
//                int cameraCount = platformStats.getInt("camera_count");
//
//                // 计算在线设备数（余煤、挂钩分割版、旋转器的count之和）
//                int onlineDevices = 0;
//                if (platformStats.has("余煤")) {
//                    onlineDevices += platformStats.getJSONObject("余煤").getInt("count");
//                }
//                if (platformStats.has("挂钩分割版")) {
//                    onlineDevices += platformStats.getJSONObject("挂钩分割版").getInt("count");
//                }
//                if (platformStats.has("旋转器")) {
//                    onlineDevices += platformStats.getJSONObject("旋转器").getInt("count");
//                }
//                //更新UI
//                updateHeaderUI(totalAlerts, unprocessedAlerts, cameraCount, onlineDevices);
//            } catch (JSONException e) {
//                Log.e("HomeFragment", "解析平台统计数据错误: " + e.getMessage());
//            }
//        } else {
//            // 发生错误时直接使用模拟数据
//             updateHeaderUI(100, 15, 15, 10);
//        }
        updateHeaderUI(100, 36, 24, 17);
    }


    // 生成报警趋势数据:根据API返回的daily_total/monthly_total数据
    private void loadAlarmTrendsData() {
        List<StatisticsData.AlarmTrendData> trendData;

//        // 尝试使用API数据
//        if (currentTimeSpan == TimeSpan.ONE_WEEK && apiDailyTotal != null) {
//            trendData = generateApiWeeklyData(apiDailyTotal);
//        } else if (currentTimeSpan == TimeSpan.ONE_YEAR && apiMonthlyTotal != null) {
//            trendData = generateApiYearlyData(apiMonthlyTotal);
//        } else {
//            // API数据不可用时直接在方法内部生成模拟数据
//            trendData = new ArrayList<>();
//            Random random = new Random();
//
//            switch (currentTimeSpan) {
//                case ONE_WEEK:
//                    String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
//                    for (String day : days) {
//                        trendData.add(new StatisticsData.AlarmTrendData(day, random.nextInt(50) + 10));
//                    }
//                    break;
//                case ONE_YEAR:
//                    String[] months = {"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
//                    for (String month : months) {
//                        trendData.add(new StatisticsData.AlarmTrendData(month, random.nextInt(100) + 20));
//                    }
//                    break;
//            }
//        }
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

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < trendData.size(); i++) {
            entries.add(new Entry(i, trendData.get(i).getAlarmCount()));
            labels.add(trendData.get(i).getTimeLabel());
        }

        LineDataSet dataSet = new LineDataSet(entries, "报警趋势");
        // --- 线条美化 ---
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);       // 设置为圆滑曲线
        dataSet.setCubicIntensity(0.2f);                      // 曲线弧度
        dataSet.setLineWidth(3f);                             // 线条宽度
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_blue)); // 线条颜色

        // --- 节点隐藏 ---
        dataSet.setDrawCircles(false);                        // 隐藏数据点圆圈 (点击时才会显示)
        dataSet.setDrawValues(false);                         // 隐藏具体的数值文字

        // --- 渐变填充 ---
        dataSet.setDrawFilled(true);                          // 启用填充
        // 尝试获取渐变Drawable，如果没有就用半透明色
        if (ContextCompat.getDrawable(requireContext(), R.drawable.fade_purple) != null) {
            dataSet.setFillDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.fade_purple));
        } else {
            dataSet.setFillColor(Color.parseColor("#204F46E5"));
        }
        // --- 设置数据 ---
        LineData lineData = new LineData(dataSet);
        chartAlarmTrends.setData(lineData);
        // --- 坐标轴与交互设置 ---

        // 1. 全局设置
        chartAlarmTrends.getDescription().setEnabled(false);  // 隐藏描述
        chartAlarmTrends.getLegend().setEnabled(false);       // 隐藏图例
        chartAlarmTrends.setScaleEnabled(false);              // 禁止缩放
        chartAlarmTrends.setDrawBorders(false);               // 去掉边框
        chartAlarmTrends.setExtraBottomOffset(10f);           // 底部留白

        // 2. X轴设置 (底部)
        XAxis xAxis = chartAlarmTrends.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);        // X轴在底部
        xAxis.setDrawGridLines(false);                        // 去掉X轴竖向网格
        xAxis.setDrawAxisLine(false);                         // 去掉X轴轴线
        xAxis.setTextColor(Color.parseColor("#999999"));      // 灰色文字
        xAxis.setTextSize(12f);
        xAxis.setGranularity(1f);                             // 强制间隔为1，防止缩放时标签重叠
        // 【关键】将上面生成的 days 或 months 列表设置给 X 轴
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        // 3. Y轴 (左侧) 设置
        YAxis leftAxis = chartAlarmTrends.getAxisLeft();
        leftAxis.setDrawAxisLine(false);                      // 去掉左侧轴线
        leftAxis.setTextColor(Color.parseColor("#999999"));   // 灰色文字
        leftAxis.setAxisMinimum(0f);                          // 从0开始
        leftAxis.enableGridDashedLine(10f, 10f, 0f);          // 启用虚线网格
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));   // 浅灰色网格

        // 4. Y轴 (右侧) 设置 - 【这里解决了你的痛点】
        chartAlarmTrends.getAxisRight().setEnabled(false);    // 彻底隐藏右侧Y轴

        // --- 刷新动画 ---
        chartAlarmTrends.animateX(1000);                      // X轴方向动画1秒
        chartAlarmTrends.invalidate();                        // 刷新重绘
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

//        // 尝试使用API数据
//        if (currentRankingTimeSpan == TimeSpan.ONE_WEEK && apiWeekTop != null) {
//            rankingData = generateApiRankingData(apiWeekTop);
//        } else if (currentRankingTimeSpan == TimeSpan.ONE_MONTH && apiMonthTop != null) {
//            rankingData = generateApiRankingData(apiMonthTop);
//        } else {
//            // API数据不可用时使用模拟数据
//            rankingData = new ArrayList<>();
//            rankingData.add(new AlarmRankingData("余煤检测", 8));
//            rankingData.add(new AlarmRankingData("旋转器检测", 5));
//            rankingData.add(new AlarmRankingData("挂钩检测分割版", 4));
//            rankingData.add(new AlarmRankingData("人员入侵监测", 2));
//        }
        rankingData = new ArrayList<>();
        rankingData.add(new AlarmRankingData("余煤检测", 25));
        rankingData.add(new AlarmRankingData("旋转器检测", 17));
        rankingData.add(new AlarmRankingData("挂钩检测分割版", 9));
        rankingData.add(new AlarmRankingData("人员入侵监测", 3));

        // ==========================================
        // 2. 直接在此处更新图表 UI
        // ==========================================

        if (chartAlarmRanking == null) return;

        if (rankingData == null || rankingData.isEmpty()) {
            chartAlarmRanking.clear();
            return;
        }
        // --- 反转数据 ---说明：rankingData 是降序（大->小），但 BarChart 默认从下往上画（Index 0 在最下）。
        // 为了让“第一名”显示在最上面，我们需要构造一个反转的列表给图表。
        List<AlarmRankingData> chartDataList = new ArrayList<>(rankingData);
        Collections.reverse(chartDataList);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> typeNames = new ArrayList<>();

        // 遍历数据填充 Entry
        for (int i = 0; i < chartDataList.size(); i++) {
            AlarmRankingData item = chartDataList.get(i);

            entries.add(new BarEntry(i, item.getValue()));
            typeNames.add(item.getName());
        }
        // --- 动态调整高度 ---
        // 基础高度 40dp + 每条数据 60dp，保证显示全且不挤
        int itemHeightDp = 60;
        int totalHeightDp = 40 + (chartDataList.size() * itemHeightDp);

        ViewGroup.LayoutParams params = chartAlarmRanking.getLayoutParams();
        params.height = dpToPx(totalHeightDp);
        chartAlarmRanking.setLayoutParams(params);

        // --- 设置数据 ---
        BarDataSet set1;
        if (chartAlarmRanking.getData() != null &&
                chartAlarmRanking.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chartAlarmRanking.getData().getDataSetByIndex(0);
            set1.setValues(entries);
            chartAlarmRanking.getData().notifyDataChanged();
            chartAlarmRanking.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(entries, "报警类型");

            // 设置单一颜色 (橙色)
            set1.setColor(Color.parseColor("#F97316"));

            // 数值样式
            set1.setValueTextSize(12f);
            set1.setValueTextColor(Color.parseColor("#666666"));
            // 整数格式化
            set1.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            data.setBarWidth(0.5f);
            chartAlarmRanking.setData(data);
        }
        // 设置 X 轴标签
        chartAlarmRanking.getXAxis().setValueFormatter(new IndexAxisValueFormatter(typeNames));
        chartAlarmRanking.getXAxis().setLabelCount(typeNames.size()); // 强制显示所有标签

        chartAlarmRanking.invalidate(); // 刷新
    }
    // 辅助方法
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void setupHorizontalChart() {
        if (chartAlarmRanking == null) return;

        chartAlarmRanking.setDrawBarShadow(false);
        chartAlarmRanking.setDrawValueAboveBar(true);
        chartAlarmRanking.getDescription().setEnabled(false); // 隐藏描述
        chartAlarmRanking.setMaxVisibleValueCount(60);
        chartAlarmRanking.setPinchZoom(false); // 禁止缩放
        chartAlarmRanking.setScaleEnabled(false);
        chartAlarmRanking.setDoubleTapToZoomEnabled(false);

        // 隐藏图例
        chartAlarmRanking.getLegend().setEnabled(false);

        // 配置 X 轴 (显示类型名称，在左侧)
        XAxis xAxis = chartAlarmRanking.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // 水平图中 BOTTOM 即左侧
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(13f);
        xAxis.setTextColor(Color.parseColor("#333333"));

        // 配置 Y 轴 (隐藏上下侧的线条和数值，只看条形)
        chartAlarmRanking.getAxisLeft().setDrawAxisLine(false);
        chartAlarmRanking.getAxisLeft().setDrawGridLines(false);
        chartAlarmRanking.getAxisLeft().setDrawLabels(false); // 不显示底部数值

        chartAlarmRanking.getAxisRight().setEnabled(false); // 隐藏右侧轴

        chartAlarmRanking.setFitBars(true); // 使条形对齐
        chartAlarmRanking.animateY(1000);   // 进场动画
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

    private void loadWeekMonthAlarmTypeData() {
        List<StatisticsData.AlarmTypeData> typeData;

//        // 尝试使用API数据
//        if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK && apiWeekTotal != null) {
//            typeData = generateAlarmTypeDataFromWeekMonthTotal(apiWeekTotal);
//            Log.d("HomeFragment", "使用API周数据生成报警类型统计");
//        } else if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_MONTH && apiMonthTotal != null) {
//            typeData = generateAlarmTypeDataFromWeekMonthTotal(apiMonthTotal);
//            Log.d("HomeFragment", "使用API月数据生成报警类型统计");
//        } else {
//            // API数据不可用时使用模拟数据
//            typeData = new ArrayList<>();
//            if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK) {
//                // 模拟周数据
//                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 3));
//                typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 5));
//                typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 2));
//            } else {
//                // 模拟月数据
//                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 12));
//                typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 18));
//                typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 8));
//            }
//            Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
//        }
        // API数据不可用时使用模拟数据
        typeData = new ArrayList<>();
//        if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK) {
//            // 模拟周数据
//            typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 3));
//            typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 5));
//            typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 2));
//        } else {
//            // 模拟月数据
//            typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 12));
//            typeData.add(new StatisticsData.AlarmTypeData("旋转器检测", 18));
//            typeData.add(new StatisticsData.AlarmTypeData("挂钩检测分割版", 8));
//        }
        if (currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK) {
            // 模拟周数据
            typeData.add(new StatisticsData.AlarmTypeData("矿井1", 3));
            typeData.add(new StatisticsData.AlarmTypeData("矿井2", 5));
            typeData.add(new StatisticsData.AlarmTypeData("矿井3", 2));
            typeData.add(new StatisticsData.AlarmTypeData("矿井4", 4));
        } else {
            // 模拟月数据
            typeData.add(new StatisticsData.AlarmTypeData("矿井1", 12));
            typeData.add(new StatisticsData.AlarmTypeData("矿井2", 18));
            typeData.add(new StatisticsData.AlarmTypeData("矿井3", 8));
            typeData.add(new StatisticsData.AlarmTypeData("矿井4", 6));
        }
        Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
        // 确保有数据可显示
        if (typeData.isEmpty()) {
            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            // PieEntry 第二个参数是标签，这里传空字符串，因为我们会用图例或外部标签显示
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
        }

        // === 2. DataSet 美化设置 (关键修改) ===
        PieDataSet dataSet = new PieDataSet(entries, ""); // 图例标签设为空，因为单独配置

        dataSet.setColors(getColorArray()); // 设置颜色

        // 间距与样式
        dataSet.setSliceSpace(3f); // 切片之间的白缝，看起来更精致
        dataSet.setSelectionShift(5f); // 点击时的突出距离

        // --- 数据线与数值位置设置 (让图表不拥挤的核心) ---
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // 标签在外部
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // 数值在外部

        // 连接线样式
        dataSet.setValueLinePart1OffsetPercentage(80.f); // 连接线起始位置
        dataSet.setValueLinePart1Length(0.3f); // 第一段线长
        dataSet.setValueLinePart2Length(0.4f); // 第二段线长
        dataSet.setValueLineWidth(5f);         // 线宽
        dataSet.setValueLineColor(Color.GRAY); // 线颜色

        // 数值文字样式
        dataSet.setValueTextSize(20f);
        dataSet.setValueTextColor(Color.BLACK); // 外部文字用黑色，清晰

        // 格式化为百分比
        dataSet.setValueFormatter(new PercentFormatter(chartWeekMonthalarmType));

        // === 3. Chart 全局设置 ===
        PieData pieData = new PieData(dataSet);
        chartWeekMonthalarmType.setData(pieData);

        // 启用百分比显示
        chartWeekMonthalarmType.setUsePercentValues(true);

        // 样式设置
        chartWeekMonthalarmType.getDescription().setEnabled(false); // 隐藏描述
        chartWeekMonthalarmType.setExtraOffsets(20.f, 0.f, 20.f, 0.f); // 【关键】设置额外边距，防止外部标签被截断

        // 圈设置
        chartWeekMonthalarmType.setDrawHoleEnabled(true);
        chartWeekMonthalarmType.setHoleColor(Color.WHITE);
        chartWeekMonthalarmType.setTransparentCircleColor(Color.WHITE);
        chartWeekMonthalarmType.setTransparentCircleAlpha(110);
        chartWeekMonthalarmType.setHoleRadius(45f); // 中间孔的大小
        chartWeekMonthalarmType.setTransparentCircleRadius(55f); // 半透明圈大小

        // 中间文字
        chartWeekMonthalarmType.setDrawCenterText(true);
        chartWeekMonthalarmType.setCenterText(currentWeekMonthAlarmTypeTimeSpan == TimeSpan.ONE_WEEK ? "本周\n分布" : "本月\n分布");
        chartWeekMonthalarmType.setCenterTextSize(18f);
        chartWeekMonthalarmType.setCenterTextColor(Color.GRAY);

        // 隐藏饼图上的 Entry Label (也就是具体的报警名称文字)，只显示数值百分比
        // 如果你想显示名称，改为 setDrawEntryLabels(true) 并设置颜色
        chartWeekMonthalarmType.setDrawEntryLabels(false);

        // === 4. 图例 Legend 设置 (美化底部图例) ===
        Legend l = chartWeekMonthalarmType.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f); // 图例间距
        l.setYEntrySpace(0f);
        l.setYOffset(10f);    // 底部抬高一点
        l.setTextSize(11f);   // 图例文字大小
        l.setTextColor(Color.parseColor("#666666")); // 灰色文字
        l.setWordWrapEnabled(true); // 允许图例换行，防止太长显示不全

        // 刷新图表
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
//    private void loadAlarmTypeStatisticsData() {
//        List<StatisticsData.AlarmTypeData> typeData;
//
//        // 尝试使用API返回的processing_info数据
//        if (processingInfo != null) {
//            typeData = generateAlarmTypeDataFromApi(processingInfo);
//            Log.d("HomeFragment", "使用API数据生成报警类型统计，类型数量: " + typeData.size());
//        } else {
//            // API数据不可用时直接在方法内生成模拟数据，不再调用独立方法
//            typeData = new ArrayList<>();
//            Random random = new Random();
//            typeData.add(new StatisticsData.AlarmTypeData("入侵报警", random.nextInt(25) + 10));
//            typeData.add(new StatisticsData.AlarmTypeData("火灾报警", random.nextInt(20) + 8));
//            typeData.add(new StatisticsData.AlarmTypeData("设备故障", random.nextInt(30) + 12));
//            typeData.add(new StatisticsData.AlarmTypeData("环境异常", random.nextInt(15) + 5));
//            typeData.add(new StatisticsData.AlarmTypeData("其他报警", random.nextInt(18) + 7));
//            Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
//        }
//
//        // 确保有数据可显示
//        if (typeData.isEmpty()) {
//            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
//            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
//        }
//
//        ArrayList<PieEntry> entries = new ArrayList<>();
//        for (StatisticsData.AlarmTypeData data : typeData) {
//            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
//            Log.d("HomeFragment", "添加报警类型: " + data.getTypeName() + ", 数量: " + data.getAlarmCount());
//        }
//
//        PieDataSet dataSet = new PieDataSet(entries, "");
//        dataSet.setColors(getColorArray());
//        dataSet.setValueTextSize(12f);
//        dataSet.setValueTextColor(Color.WHITE);
//
//        // 添加整数值格式化器
//        dataSet.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return String.valueOf((int) value);
//            }
//        });
//
//        PieData pieData = new PieData(dataSet);
//        chartTypeStatistics.setData(pieData);
//
//        // Customize chart appearance
//        chartTypeStatistics.getDescription().setEnabled(false);
//        chartTypeStatistics.setDrawHoleEnabled(true);
//        chartTypeStatistics.setHoleColor(Color.WHITE);
//        chartTypeStatistics.setTransparentCircleRadius(61f);
//        chartTypeStatistics.setHoleRadius(58f);
//        chartTypeStatistics.setDrawCenterText(true);
//        chartTypeStatistics.setCenterText("类型分布");
//        chartTypeStatistics.setCenterTextSize(16f);
//        chartTypeStatistics.getLegend().setEnabled(true);
//        chartTypeStatistics.getLegend().setTextSize(12f);
//
//        chartTypeStatistics.invalidate();
//    }
//
//    // 生成报警类型统计数据:根据API返回的processing_info数据
//    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromApi(JSONObject processingInfo) {
//        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();
//        Map<String, Integer> typeCountMap = new HashMap<>();
//
//        try {
//            // 检查processing_info是否包含list字段
//            if (processingInfo.has("list")) {
//                JSONArray alarmList = processingInfo.getJSONArray("list");
//                Log.d("HomeFragment", "processing_info.list长度: " + alarmList.length());
//
//                // 统计每种报警类型的数量
//                for (int i = 0; i < alarmList.length(); i++) {
//                    JSONObject alarmItem = alarmList.getJSONObject(i);
//                    if (alarmItem.has("type")) {
//                        String type = alarmItem.getString("type");
//                        typeCountMap.put(type, typeCountMap.getOrDefault(type, 0) + 1);
//                        Log.d("HomeFragment", "处理报警项 " + i + ": type=" + type);
//                    }
//                }
//
//                // 将统计结果转换为AlarmTypeData列表
//                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
//                    typeData.add(new StatisticsData.AlarmTypeData(entry.getKey(), entry.getValue()));
//                    Log.d("HomeFragment", "添加统计结果: " + entry.getKey() + " = " + entry.getValue());
//                }
//            }
//        } catch (JSONException e) {
//            Log.e("HomeFragment", "解析processing_info数据失败: " + e.getMessage());
//        }
//
//        return typeData;
//    }

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