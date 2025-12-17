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
import com.example.mineguard.data.DeviceItem; // 1. 引入设备数据模型

// 新增 Glide 和 File 的导入
import com.bumptech.glide.Glide;
import java.io.File;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.widget.Toast;
import java.util.Date;
import android.widget.PopupWindow;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

/**
 * 报警列表适配器 - 已修改：支持显示报警抓拍图片
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmList;
    private List<DeviceItem> deviceList; // 保存系统配置的设备列表
    private OnAlarmClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnAlarmClickListener {
        void onAlarmClick(AlarmItem alarm);
        void onAlarmLongClick(AlarmItem alarm);
        void onAlarmStatusChanged(AlarmItem alarm);
    }

    public AlarmAdapter(List<AlarmItem> alarmList, OnAlarmClickListener listener) {
        this.alarmList = alarmList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }

    // 外部 Fragment 调用此方法传入最新的设备列表
    public void setDeviceList(List<DeviceItem> list) {
        this.deviceList = list;
        notifyDataSetChanged(); // 数据更新后刷新列表显示
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        private View viewLevelIndicator;
        private ImageView imageView;
        private TextView tvTitle;
        private TextView tvScene;
        private TextView tvStatusBadge;
        private TextView tvTime;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLevelIndicator = itemView.findViewById(R.id.viewLevelIndicator);
            imageView = itemView.findViewById(R.id.imageView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvScene = itemView.findViewById(R.id.tvScene);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(AlarmItem alarm) {
            Context context = itemView.getContext();

            // --- 1. 设置标题 ---
            String type = alarm.getType() != null ? alarm.getType() : "未知报警";
            String title = type + " #" + alarm.getId();
            tvTitle.setText(title);

            // --- 2. 匹配设备名称和区域 ---
            String showName = "";
            String showArea = "";
            String ip = alarm.getIp(); // 获取报警信息的 IP
            boolean isMatched = false;

            // 遍历配置列表进行匹配
            if (deviceList != null && ip != null) {
                for (DeviceItem device : deviceList) {
                    // 忽略大小写和空格比对 IP
                    if (device.getIpAddress() != null && device.getIpAddress().trim().equalsIgnoreCase(ip.trim())) {
                        showName = device.getDeviceName();
                        showArea = device.getArea();
                        isMatched = true;
                        break;
                    }
                }
            }

            // 根据匹配结果设置文本
            if (isMatched) {
                tvScene.setText(showArea + " | " + showName);
            } else {
                String location = alarm.getLocation() != null ? alarm.getLocation() : "未知位置";
                if (ip != null && !ip.isEmpty()) {
                    tvScene.setText(location + " | " + ip);
                } else {
                    tvScene.setText(location);
                }
            }

            // --- 3. 设置时间 ---
            String timeStr = alarm.getSolve_time();
            if (timeStr != null && !timeStr.isEmpty()) {
                tvTime.setText(timeStr);
            } else {
                tvTime.setText("待处理");
            }

            // --- 4. 图片加载 (核心修改) ---
            String path = alarm.getPath();
            if (path != null && !path.isEmpty()) {
                Object imageSource;
                // 判断是绝对路径还是其他类型的路径（如 URI）
                if (path.startsWith("/")) {
                    imageSource = new File(path);
                } else {
                    imageSource = path;
                }

                // 使用 Glide 加载图片
                Glide.with(context)
                        .load(imageSource)
                        .placeholder(R.drawable.ic_alarm_empty) // 加载中显示的占位图
                        .error(R.drawable.ic_alarm_empty)       // 加载失败显示的错误图
                        .centerCrop()                           // 裁剪图片以填充 ImageView
                        .into(imageView);
            } else {
                // 如果没有图片路径，显示默认图标
                imageView.setImageResource(R.drawable.ic_alarm_empty);
            }

            // --- 5. 样式与事件 ---
            setupStyle(alarm);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlarmClick(alarm);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onAlarmLongClick(alarm);
                return true;
            });
        }

        private void setupStyle(AlarmItem alarm) {
            int levelColor = alarm.getLevelColor();
            if (levelColor == 0) levelColor = 0xFFD32F2F;

            viewLevelIndicator.setBackgroundColor(levelColor);

            int status = alarm.getStatus();
            switch (status) {
                case 1:
                    tvStatusBadge.setText("已处理");
                    setStatusBadgeStyle(0xFF43A047); // 绿
                    tvStatusBadge.setOnClickListener(null);
                    break;
                case 2:
                    tvStatusBadge.setText("误报");
                    setStatusBadgeStyle(0xFFFF9800); // 橙
                    tvStatusBadge.setOnClickListener(null);
                    break;
                case 0:
                default:
                    tvStatusBadge.setText("未处理▼");
                    setStatusBadgeStyle(0xFFD32F2F); // 红
                    tvStatusBadge.setOnClickListener(v -> {
                        showCustomStatusPopup(v, alarm, getBindingAdapterPosition());
                    });
                    break;
            }
        }

        private void showCustomStatusPopup(View anchorView, AlarmItem alarm, int position) {
            Context context = anchorView.getContext();
            View popupView = LayoutInflater.from(context).inflate(R.layout.layout_status_popup, null);

            final PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setElevation(10);
            popupWindow.setOutsideTouchable(true);

            TextView tvProcessed = popupView.findViewById(R.id.tvMenuProcessed);
            TextView tvFalseAlarm = popupView.findViewById(R.id.tvMenuFalseAlarm);
            String currentTime = dateFormat.format(new Date());

            tvProcessed.setOnClickListener(v -> {
                alarm.setStatus(AlarmItem.STATUS_PROCESSED);
                alarm.setSolve_time(currentTime);
                updateItem(position);
                if (listener != null) listener.onAlarmStatusChanged(alarm);
                Toast.makeText(context, "已标记为已处理", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            tvFalseAlarm.setOnClickListener(v -> {
                alarm.setStatus(AlarmItem.STATUS_FALSE_ALARM);
                alarm.setSolve_time(currentTime);
                updateItem(position);
                if (listener != null) listener.onAlarmStatusChanged(alarm);
                Toast.makeText(context, "已标记为误报", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            popupWindow.showAsDropDown(anchorView, 0, 4);
        }

        private void updateItem(int position) {
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        private void setStatusBadgeStyle(int color) {
            android.graphics.drawable.Drawable background = tvStatusBadge.getBackground();
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dpToPx(12));
                drawable.setColor(color);
                tvStatusBadge.setBackground(drawable);
            }
        }

        private int dpToPx(int dp) {
            return (int) (itemView.getContext().getResources().getDisplayMetrics().density * dp);
        }
    }
}