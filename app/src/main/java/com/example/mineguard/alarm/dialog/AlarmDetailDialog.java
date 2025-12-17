package com.example.mineguard.alarm.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider; // 1. 导入 ViewModel 相关
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.data.DeviceItem; // 2. 导入数据模型
import com.example.mineguard.data.DeviceViewModel; // 3. 导入 ViewModel
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

/**
 * 报警详情弹窗
 * 修改记录：已关联系统配置中的设备列表，根据 IP 动态显示名称和区域
 */
public class AlarmDetailDialog extends DialogFragment {

    private ImageView imageView;
    private TextView tvAlarmID, tvTime, tvAlgorithmID, tvArea, tvType, tvIP;
    private Button btnClose;
    private AlarmItem alarm;

    // 增加 ViewModel 成员变量
    private DeviceViewModel deviceViewModel;

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

        // 4. 初始化 ViewModel (使用 requireActivity() 以便与配置页共享数据)
        deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_alarm_detail, container, false);

        imageView = view.findViewById(R.id.imageView);
        tvAlarmID = view.findViewById(R.id.tvAlarmID);
        tvType = view.findViewById(R.id.tvType);
        tvTime = view.findViewById(R.id.tvTime);
        tvAlgorithmID = view.findViewById(R.id.tvAlgorithmID);
        tvIP = view.findViewById(R.id.tvIP);   // 界面上显示的标签是“关联摄像头”，对应变量名 tvIP
        tvArea = view.findViewById(R.id.tvArea);
        btnClose = view.findViewById(R.id.btnClose);

        setupData();

        btnClose.setOnClickListener(v -> dismiss());
        return view;
    }

    private void setupData() {
        if (alarm == null) return;

        tvAlarmID.setText(alarm.getId() != 0 ? String.valueOf(alarm.getId()) : "未知目标");
        tvType.setText(alarm.getType() != null ? alarm.getType() : "未知类型");
        tvAlgorithmID.setText(alarm.getAlgorithm_code() != null ? alarm.getAlgorithm_code() : "通用算法");
        tvTime.setText(alarm.getSolve_time() != null ? alarm.getSolve_time() : "未知时间");

        // === 修改核心逻辑：根据 IP 从设备列表中查找信息 ===
        String alarmIp = alarm.getIp(); // 获取报警数据中的 IP (或 extend 字段)
        boolean isMatched = false;

        // 获取当前的设备列表数据
        List<DeviceItem> deviceList = deviceViewModel.getLiveDeviceList().getValue();

        if (alarmIp != null && deviceList != null) {
            for (DeviceItem device : deviceList) {
                // 忽略大小写和空格进行比对
                if (device.getIpAddress() != null &&
                        device.getIpAddress().trim().equalsIgnoreCase(alarmIp.trim())) {

                    // 1. 关联摄像头 -> 显示系统配置里的“设备名称”
                    tvIP.setText(device.getDeviceName());

                    // 2. 位置信息 -> 显示系统配置里的“所属区域”
                    tvArea.setText(device.getArea());

                    isMatched = true;
                    break; // 找到后退出循环
                }
            }
        }

        // 如果在配置列表中没找到匹配的 IP，显示默认信息或原始 IP
        if (!isMatched) {
            tvIP.setText(alarmIp != null ? alarmIp : "未知摄像头");
            tvArea.setText("未知区域");
        }

        // === 图片加载保持不变 ===
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

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}