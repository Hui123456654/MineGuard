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
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.bumptech.glide.Glide;
import java.io.File;

/**
 * 报警详情弹窗
 */
public class AlarmDetailDialog extends DialogFragment {

    private ImageView imageView;
    // 定义控件变量
    private TextView tvAlarmID, tvTime, tvAlgorithmID,  tvArea,tvType,tvIP;
    private Button btnClose;
    private AlarmItem alarm;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_alarm_detail, container, false);

        // 1. 绑定控件
        imageView = view.findViewById(R.id.imageView);

        // 对应 XML 里的 "报警ID:"
        tvAlarmID = view.findViewById(R.id.tvAlarmID);
        //对应 报警类型
        tvType = view.findViewById(R.id.tvType);
        // 对应 XML 里的 "设备IP地址" (实际将显示时间)
        tvTime = view.findViewById(R.id.tvTime);

        // 对应 XML 里的 "算法编码:"
        tvAlgorithmID = view.findViewById(R.id.tvAlgorithmID);

        // 对应 XML 里的 "关联摄像头:"
        tvIP = view.findViewById(R.id.tvIP);

        // 对应 XML 里的 "位置信息:"
        tvArea = view.findViewById(R.id.tvArea);

        btnClose = view.findViewById(R.id.btnClose);

        setupData();

        btnClose.setOnClickListener(v -> dismiss());
        return view;
    }

    private void setupData() {
        if (alarm == null) return;

        // ✅ 正确写法：使用 String.valueOf() 将数字转为字符串
        tvAlarmID.setText(alarm.getId() != 0 ? String.valueOf(alarm.getId()) : "未知目标");
        tvType.setText(alarm.getType() != null ? alarm.getType() : "未知类型");

        tvAlgorithmID.setText(alarm.getAlgorithm_code() != null ? alarm.getAlgorithm_code() : "通用算法");
        tvIP.setText(alarm.getIp() != null ? alarm.getIp() : "未知IP");
        tvTime.setText(alarm.getSolve_time() != null ? alarm.getSolve_time() : "未知时间");

        String extendInfo = alarm.getIp();


        if (extendInfo != null) {
            if (extendInfo.contains("CAM_01")) {
                tvIP.setText("摄像头 1");
                tvArea.setText("东区 (挖掘面)");
            } else if (extendInfo.contains("CAM_02")) {
                tvIP.setText("摄像头 2");
                tvArea.setText("西区 (传送带)");
            } else if (extendInfo.contains("MobileApp")) {
                tvIP.setText("手机测试");
                tvArea.setText("调试区域");
            } else {
                tvIP.setText(extendInfo); // 直接显示扩展信息
                tvArea.setText("未知区域");
            }
        } else {
            tvIP.setText("未知摄像头");
            tvArea.setText("未知区域");
        }

        // === 5. 图片加载 ===
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