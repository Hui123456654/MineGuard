//package com.example.mineguard.home;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.RectF;
//import android.util.AttributeSet;
//import android.view.View;
//import androidx.annotation.Nullable;
//import com.example.mineguard.home.HomeFragment.AlarmRankingData; // 引用 HomeFragment 中的静态内部类
//import java.util.ArrayList;
//import java.util.List;
//
//public class RadialRankingView extends View {
//
//    private List<AlarmRankingData> dataList = new ArrayList<>();
//    private Paint paint;
//    private RectF rectF;
//
//    // 配色方案：红 -> 橙 -> 黄 -> 绿 -> 蓝 -> 紫 (根据排名热度)
//    private final int[] palette = {
//            Color.parseColor("#EF4444"), // Red
//            Color.parseColor("#F97316"), // Orange
//            Color.parseColor("#FBBF24"), // Amber
//            Color.parseColor("#10B981"), // Emerald
//            Color.parseColor("#3B82F6"), // Blue
//            Color.parseColor("#6366F1")  // Indigo
//    };
//
//    private float strokeWidth = 20f;
//    private float gapWidth = 10f;
//    private int maxCount = 1;
//
//    public RadialRankingView(Context context) { super(context); init(); }
//    public RadialRankingView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
//
//    private void init() {
//        paint = new Paint();
//        paint.setAntiAlias(true);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeCap(Paint.Cap.ROUND);
//        rectF = new RectF();
//    }
//
//    public void setData(List<AlarmRankingData> list) {
//        this.dataList = list;
//        // 因为你的数据已经是降序的，取第一个作为最大值基准
//        if (!list.isEmpty()) {
//            maxCount = list.get(0).getValue();
//            if (maxCount == 0) maxCount = 1;
//        }
//        invalidate();
//    }
//
//    // 提供给 HomeFragment 获取对应索引的颜色
//    public int getColorForIndex(int index) {
//        return palette[index % palette.length];
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        if (dataList == null || dataList.isEmpty()) return;
//
//        float centerX = getWidth() / 2f;
//        float centerY = getHeight() / 2f;
//        float availableRadius = Math.min(centerX, centerY) - 10f; // 留边距
//
//        int count = Math.min(dataList.size(), palette.length); // 最多只画颜色数量的圈
//
//        // 动态计算线宽，保证能画下所有圈
//        float totalSpacePerItem = availableRadius / (float) count;
//        strokeWidth = totalSpacePerItem * 0.7f; // 线宽占70%
//        gapWidth = totalSpacePerItem * 0.3f;    // 间距占30%
//
//        // 从外向内画 (i=0是第一名，在外圈)
//        for (int i = 0; i < count; i++) {
//            AlarmRankingData item = dataList.get(i);
//
//            // 半径计算：最大半径 - (层级 * (宽+间距)) - 半个线宽
//            float currentRadius = availableRadius - (i * (strokeWidth + gapWidth)) - (strokeWidth / 2);
//
//            rectF.set(centerX - currentRadius, centerY - currentRadius,
//                    centerX + currentRadius, centerY + currentRadius);
//
//            // 1. 画灰色底轨
//            paint.setColor(Color.parseColor("#F3F4F6"));
//            paint.setStrokeWidth(strokeWidth);
//            canvas.drawArc(rectF, 0, 360, false, paint);
//
//            // 2. 画彩色进度
//            // 角度计算：基于最大值的比例。第一名接近 360 度，其他的按比例缩小
//            float sweepAngle = (item.getValue() / (float) maxCount) * 360f;
//            // 视觉微调：如果是满分，稍微留一点缺口好看，或者直接360
//            if (sweepAngle > 360) sweepAngle = 360;
//
//            paint.setColor(palette[i % palette.length]);
//            canvas.drawArc(rectF, -90, sweepAngle, false, paint); // -90表示从12点方向开始
//        }
//    }
//}