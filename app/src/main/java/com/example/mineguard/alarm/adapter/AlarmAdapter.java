package com.example.mineguard.alarm.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import android.widget.PopupMenu;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.widget.Toast;
import java.util.Date;
import android.widget.PopupWindow;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
/**
 * 报警列表适配器 - 配合美化后的 item_alarm_card.xml
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmList;
    private OnAlarmClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnAlarmClickListener {
        void onAlarmClick(AlarmItem alarm);
        void onAlarmLongClick(AlarmItem alarm);
        // 新增：当在列表中直接修改状态时调用
        void onAlarmStatusChanged(AlarmItem alarm);
    }

    public AlarmAdapter(List<AlarmItem> alarmList, OnAlarmClickListener listener) {
        this.alarmList = alarmList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 确保这里加载的是我们要修改的那个 layout 文件
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_card, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarm = alarmList.get(position);
        holder.bind(alarm);
    }

    @Override
    public int getItemCount() {
        return alarmList != null ? alarmList.size() : 0;
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        // 声明新布局中的控件
        private View viewLevelIndicator; // 左侧颜色条
        private ImageView imageView;     // 图片
        private TextView tvTitle;        // 标题 (类型 + 名称)
        private TextView tvScene;        // 场景/位置
        private TextView tvStatusBadge;  // 右上角状态胶囊
        private TextView tvTime;         // 底部时间

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定 item_alarm_card.xml 中的新 ID
            viewLevelIndicator = itemView.findViewById(R.id.viewLevelIndicator);
            imageView = itemView.findViewById(R.id.imageView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvScene = itemView.findViewById(R.id.tvScene);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(AlarmItem alarm) {
            Context context = itemView.getContext();

            // --- 1. 设置文本信息 ---

            // 标题：组合显示 "报警类型 #ID"，看起来更专业
            String type = alarm.getType() != null ? alarm.getType() : "未知报警";
            String title = type + " #" + alarm.getId();
            tvTitle.setText(title);

            // 场景/位置：组合位置和IP，增加信息密度
            String location = alarm.getLocation() != null ? alarm.getLocation() : "未知位置";
            String ip = (alarm.getIp() != null && !alarm.getIp().isEmpty()) ? alarm.getIp() : "";
            if (!ip.isEmpty()) {
                tvScene.setText(location + " | " + ip);
            } else {
                tvScene.setText(location);
            }

            // --- 设置时间 ---
            String timeStr = alarm.getSolve_time();
            if (timeStr != null && !timeStr.isEmpty()) {
                tvTime.setText(timeStr);
            } else {
                tvTime.setText("待处理");
            }

            // --- 2. 设置图片 (Glide 部分) ---

            // 假设你的 AlarmItem 有一个 getImageUrl() 方法。如果没有，我们默认显示本地图标。
            // 这里的 R.drawable.ic_alarm_empty 请替换为你项目中真实的默认图

            // 如果你已添加 Glide 依赖，请使用以下代码：

//            if (alarm.getPath() != null && !alarm.getPath().isEmpty()) {
//                Glide.with(context)
//                     .load(alarm.getPath())
//                     .centerCrop()
//                     .placeholder(R.drawable.ic_alarm_empty)
//                     .into(imageView);
//            } else {
//                imageView.setImageResource(R.drawable.ic_alarm_empty);
//            }


            // 暂时使用的标准代码 (不依赖 Glide，直接设置资源，保证代码不报错)
            imageView.setImageResource(R.drawable.ic_alarm_empty);


            // --- 3. 美化样式逻辑 (核心部分) ---
            setupStyle(alarm);

            // --- 4. 点击事件 ---
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlarmClick(alarm);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onAlarmLongClick(alarm);
                return true;
            });
        }

        /**
         * 专门处理颜色和状态的方法，保持 bind 代码整洁
         */
        private void setupStyle(AlarmItem alarm) {
            // 获取报警等级颜色，如果 AlarmItem 没有 getLevelColor，可以自己定义逻辑
            int levelColor = alarm.getLevelColor();
            // 如果 getLevelColor 返回 0 或无效值，给个默认红色
            if (levelColor == 0) levelColor = 0xFFD32F2F;

            // A. 设置左侧指示条颜色
            viewLevelIndicator.setBackgroundColor(levelColor);

            // B. 设置状态标签 (Badge)
            int status = alarm.getStatus();

            switch (status) {
                case 1:
                    tvStatusBadge.setText("已处理");
                    setStatusBadgeStyle(0xFF43A047);// 绿色
                    tvStatusBadge.setOnClickListener(null);
                    break;

                case 2:
                    tvStatusBadge.setText(" 误报 ");
                    setStatusBadgeStyle(0xFFFF9800);// 橙色
                    tvStatusBadge.setOnClickListener(null);
                    break;

                case 0:
                default:
                    tvStatusBadge.setText("未处理▼");
                    setStatusBadgeStyle(0xFFD32F2F); // 未处理：红
                    tvStatusBadge.setOnClickListener(v -> {
                        showCustomStatusPopup(v, alarm, getBindingAdapterPosition());
                    });
                    break;
            }
        }

        /**
         * 显示自定义的美化版状态选择弹窗
         */
        private void showCustomStatusPopup(View anchorView, AlarmItem alarm, int position) {
            Context context = anchorView.getContext();

            // 1. 加载自定义布局
            View popupView = LayoutInflater.from(context).inflate(R.layout.layout_status_popup, null);

            // 2. 创建 PopupWindow
            // 宽度固定，高度自适应
            final PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            // 3. 设置背景透明（为了让自定义布局的圆角和阴影生效）
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 设置阴影（Android 5.0+）
            popupWindow.setElevation(10);
            popupWindow.setOutsideTouchable(true);

            // 4. 获取布局中的控件并设置点击事件
            TextView tvProcessed = popupView.findViewById(R.id.tvMenuProcessed);
            TextView tvFalseAlarm = popupView.findViewById(R.id.tvMenuFalseAlarm);

            String currentTime = dateFormat.format(new Date());

            // 点击“标记为已处理”
            tvProcessed.setOnClickListener(v -> {
                alarm.setStatus(AlarmItem.STATUS_PROCESSED);
                alarm.setSolve_time(currentTime);
                updateItem(position); // 封装刷新逻辑
                // 2. 【关键新增】通知 Fragment 数据变了，快去保存！
                if (listener != null) {
                    listener.onAlarmStatusChanged(alarm);
                }
                Toast.makeText(context, "已标记为已处理", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            // 点击“标记为误报”
            tvFalseAlarm.setOnClickListener(v -> {
                alarm.setStatus(AlarmItem.STATUS_FALSE_ALARM);
                alarm.setSolve_time(currentTime);
                updateItem(position); // 封装刷新逻辑

                // 2. 【关键新增】通知 Fragment 数据变了
                if (listener != null) {
                    listener.onAlarmStatusChanged(alarm);
                }
                Toast.makeText(context, "已标记为误报", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            // 5. 显示在 anchorView (状态胶囊) 的下方
            // xoff: 0, yoff: 4 (稍微向下一点点，不遮挡胶囊)
            popupWindow.showAsDropDown(anchorView, 0, 4);
        }
        // 辅助方法：安全刷新 Item
        private void updateItem(int position) {
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        /**
         * 动态修改状态标签的圆角背景颜色
         */
        private void setStatusBadgeStyle(int color) {
            // 获取 XML 中定义的背景 drawable
            android.graphics.drawable.Drawable background = tvStatusBadge.getBackground();
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dpToPx(12)); // 设置圆角 12dp
                drawable.setColor(color);
                tvStatusBadge.setBackground(drawable);
            }
        }

        // dp 转 px 辅助工具
        private int dpToPx(int dp) {
            return (int) (itemView.getContext().getResources().getDisplayMetrics().density * dp);
        }
    }
}