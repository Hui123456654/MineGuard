package com.example.mineguard.alarm.dialog;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu; // 1. 导入 PopupMenu
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.data.DeviceItem;
import com.example.mineguard.data.DeviceViewModel;
import com.bumptech.glide.Glide;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.DisplayMetrics;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

/**
 * 报警详情弹窗
 * 修改记录：增加状态修改功能，颜色与列表页保持一致
 */
public class AlarmDetailDialog extends DialogFragment {

    private ImageView imageView;
    private TextView tvAlarmID, tvTime, tvAlgorithmID, tvArea, tvType, tvIP, tvLevel, tvStatus;
    private Button btnClose;
    private AlarmItem alarm;

    // ViewModel 用于获取设备配置信息
    private DeviceViewModel deviceViewModel;

    // 2. 定义状态改变监听接口
    private OnStatusChangeListener statusListener;

    public interface OnStatusChangeListener {
        void onStatusChanged(AlarmItem alarm);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        this.statusListener = listener;
    }

    public static AlarmDetailDialog newInstance(AlarmItem alarm) {
        AlarmDetailDialog dialog = new AlarmDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable("alarm", alarm);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            alarm = (AlarmItem) getArguments().getSerializable("alarm");
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
        deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_alarm_detail, container, false);

        imageView = view.findViewById(R.id.imageView);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvAlarmID = view.findViewById(R.id.tvAlarmID);
        tvType = view.findViewById(R.id.tvType);
        tvTime = view.findViewById(R.id.tvTime);
        tvAlgorithmID = view.findViewById(R.id.tvAlgorithmID);
        tvIP = view.findViewById(R.id.tvIP);
        tvArea = view.findViewById(R.id.tvArea);
        btnClose = view.findViewById(R.id.btnClose);

        setupData();

        btnClose.setOnClickListener(v -> dismiss());

        // 3. 给状态标签添加点击事件
        tvStatus.setOnClickListener(this::showStatusPopup);

        return view;
    }

    private void setupData() {
        if (alarm == null) return;

        tvAlarmID.setText(String.valueOf(alarm.getId()));
        tvType.setText(alarm.getType() != null ? alarm.getType() : "未知类型");
        tvAlgorithmID.setText(alarm.getAlgorithm_code() != null ? alarm.getAlgorithm_code() : "通用算法");
        tvTime.setText(alarm.getSolve_time() != null ? alarm.getSolve_time() : "未知时间");

        // 设置等级
        if (alarm.getLevel() == AlarmItem.LEVEL_CRITICAL) {
            tvLevel.setText("严重");
            // 严重等级通常也是红色或深红，这里保持默认或按需设置
        } else {
            tvLevel.setText("警告");
        }

        // 4. 初始化状态样式
        updateStatusUI();

        // 匹配设备信息逻辑
        String alarmIp = alarm.getIp();
        boolean isMatched = false;
        List<DeviceItem> deviceList = deviceViewModel.getLiveDeviceList().getValue();
        if (alarmIp != null && deviceList != null) {
            for (DeviceItem device : deviceList) {
                if (device.getIpAddress() != null &&
                        device.getIpAddress().trim().equalsIgnoreCase(alarmIp.trim())) {
                    tvIP.setText(device.getDeviceName());
                    tvArea.setText(device.getArea());
                    isMatched = true;
                    break;
                }
            }
        }
        if (!isMatched) {
            tvIP.setText(alarmIp != null ? alarmIp : "未知摄像头");
            tvArea.setText(alarm.getLocation() != null ? alarm.getLocation() : "未知区域");
        }

        // 图片加载
        String path = alarm.getPath();
        if (path != null && !path.isEmpty()) {
            Object imageSource;
            if (path.startsWith("/")) {
                imageSource = new File(path);
            } else {
                imageSource = path;
            }
            Glide.with(this)
                    .load(imageSource)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(imageView);
        }
    }

    /**
     * 5. 核心方法：更新状态显示的文字和背景颜色
     * 颜色代码与 AlarmAdapter 保持完全一致
     */
    private void updateStatusUI() {
        int status = alarm.getStatus();
        int color;
        String text;

        switch (status) {
            case 1: // 已处理
                text = "已处理";
                color = 0xFF43A047; // 绿色
                break;
            case 2: // 误报
                text = "误报";
                color = 0xFFFF9800; // 橙色
                break;
            case 0: // 未处理
            default:
                text = "未处理 ▼"; // 加个箭头提示可点击
                color = 0xFFD32F2F; // 红色
                break;
        }

        tvStatus.setText(text);
        setStatusBadgeStyle(color);
    }

    /**
     * 动态设置圆角背景颜色
     */
    private void setStatusBadgeStyle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(12)); // 12dp 圆角
        drawable.setColor(color);
        tvStatus.setBackground(drawable);
        // 保证文字颜色是白色，以适应深色背景
        tvStatus.setTextColor(0xFFFFFFFF);
    }

    /**
     * 6. 显示状态选择菜单
     */
    private void showStatusPopup(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 0, 0, "未处理");
        popup.getMenu().add(0, 1, 0, "已处理");
        popup.getMenu().add(0, 2, 0, "标记为误报");

        popup.setOnMenuItemClickListener(item -> {
            int newStatus = 0;
            String title = item.getTitle().toString();

            if (title.contains("已处理")) newStatus = 1;
            else if (title.contains("误报")) newStatus = 2;
            else newStatus = 0;

            if (alarm.getStatus() != newStatus) {
                // 更新数据对象
                alarm.setStatus(newStatus);
                // 如果是处理或误报，更新处理时间
                if (newStatus != 0) {
                    String timeStr = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date());
                    alarm.setSolve_time(timeStr);
                }

                // 更新界面
                updateStatusUI();

                // 通知 Fragment 保存更改
                if (statusListener != null) {
                    statusListener.onStatusChanged(alarm);
                }
                Toast.makeText(getContext(), "状态已更新: " + title, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }

    private int dpToPx(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // 1. 获取屏幕的真实尺寸
            DisplayMetrics dm = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

            // 2. 动态计算：宽度占屏幕 90%，高度占屏幕 80%
            // 这样无论是在手机还是平板上，它都会是一个很大的大弹窗
            int width = (int) (dm.widthPixels * 0.9);
            int height = (int) (dm.heightPixels * 0.8);

            // 3. 应用尺寸
            getDialog().getWindow().setLayout(width, height);

            // 4. (可选) 设置背景透明，这样圆角效果才不会有白边
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}