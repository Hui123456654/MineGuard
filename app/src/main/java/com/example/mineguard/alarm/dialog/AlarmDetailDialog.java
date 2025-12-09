package com.example.mineguard.alarm.dialog;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.bumptech.glide.Glide;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import static com.example.mineguard.MyApplication.globalIP;
import com.bumptech.glide.Glide;
import java.io.File;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * 报警详情对话框
 */
public class AlarmDetailDialog extends DialogFragment {
    
    private ImageView imageView;
    private TextView tvLevel;
    private TextView tvDeviceName;
    private TextView tvAlgorithmType;
    private TextView tvScene;
    private TextView tvArea;
    private TextView tvTime;
    private TextView tvStatus;
    private TextView tvProcessInfo;
    private TextView tvProcessor;
    private TextView tvProcessTime;
    private EditText etProcessInfo;
    private Button btnSave;
    private Button btnExport;
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
        Bundle args = getArguments();
        if (args != null) {
            alarm = (AlarmItem) args.getSerializable("alarm");
        }
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_alarm_detail, container, false);
        
        initViews(view);
        setupData();
        setupClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        imageView = view.findViewById(R.id.imageView);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        tvAlgorithmType = view.findViewById(R.id.tvAlgorithmType);
        tvScene = view.findViewById(R.id.tvScene);
        tvArea = view.findViewById(R.id.tvArea);
        tvTime = view.findViewById(R.id.tvTime);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvProcessInfo = view.findViewById(R.id.tvProcessInfo);
        tvProcessor = view.findViewById(R.id.tvProcessor);
        tvProcessTime = view.findViewById(R.id.tvProcessTime);
        etProcessInfo = view.findViewById(R.id.etProcessInfo);
        btnSave = view.findViewById(R.id.btnSave);
        btnExport = view.findViewById(R.id.btnExport);
        btnClose = view.findViewById(R.id.btnClose);
    }
    
    // 在setupData方法中修改图片加载逻辑
    private void setupData() {
        if (alarm == null) return;
        
        // 设置图片 - 从AlarmItem的path属性加载，添加前缀
        String path = alarm.getPath();
        if (path != null && !path.isEmpty()) {
            // 构建完整的图片URL
            String imageUrl = "http://" + globalIP + ":5004/data/media/" + path;
            
            // 使用Glide加载图片
            Glide.with(this)
                 .load(imageUrl)
                 .placeholder(android.R.drawable.ic_dialog_alert) // 加载占位图
                 .error(android.R.drawable.ic_dialog_alert) // 错误占位图
                 .into(imageView);
        } else {
            // 如果没有路径，使用默认图标
            imageView.setImageResource(android.R.drawable.ic_dialog_alert);
        }
        
        // 设置基本信息
        tvLevel.setText(alarm.getLevelDescription());
        tvLevel.setTextColor(alarm.getLevelColor());
        // 根据警告级别设置背景样式
        if (alarm.isCritical()) {
            tvLevel.setBackgroundResource(R.drawable.bg_level_critical); // 严重级别 - 红色边框
        } else {
            tvLevel.setBackgroundResource(R.drawable.bg_level_warning); // 警告级别 - 橙色边框
        }
        
        // 1. 报警ID：对应AlarmItem的id属性
        tvDeviceName.setText(String.valueOf(alarm.getId()));
        
        // 2. 报警类型：对应AlarmItem的type属性
        tvAlgorithmType.setText(alarm.getType() != null ? alarm.getType() : "未知类型");
        
        // 3. 关联摄像头：对应AlarmItem的camera_id属性
        Integer cameraId = alarm.getCamera_id();
        tvScene.setText(cameraId != null ? "摄像头" + cameraId : "未知摄像头");
        
        // 4. 位置信息：对应AlarmItem的location属性
        tvArea.setText(alarm.getLocation() != null ? alarm.getLocation() : "未知位置");
        
        // 5. 设备IP地址：对应AlarmItem的ip属性
        tvTime.setText(alarm.getIp() != null ? alarm.getIp() : "未知IP地址");
        
        // 设置状态
        int statusValue = alarm.getStatus();
        switch (statusValue) {
            case 1:
                tvStatus.setText("已处理");
                tvStatus.setTextColor(0xFF43A047);
                tvStatus.setBackgroundResource(R.drawable.bg_status_processed); // 绿色边框
                break;

            case 2:
                tvStatus.setText(" 误报 ");
                tvStatus.setTextColor(0xFFFF9800);
                tvStatus.setBackgroundResource(R.drawable.bg_status_falsealarm); // cheng色边框
                break;

            case 0:
            default:
                tvStatus.setText("未处理");
                tvStatus.setTextColor(0xFFD32F2F);
                tvStatus.setBackgroundResource(R.drawable.bg_status_unprocessed); // 红色边框
                break;
        }
        
        // 设置处理信息
        if (alarm.getProcessInfo() != null && !alarm.getProcessInfo().isEmpty()) {
            tvProcessInfo.setText(alarm.getProcessInfo());
            tvProcessInfo.setVisibility(View.VISIBLE);
            etProcessInfo.setVisibility(View.GONE);
            btnSave.setText("修改处理信息");
        } else {
            tvProcessInfo.setVisibility(View.GONE);
            etProcessInfo.setVisibility(View.VISIBLE);
            btnSave.setText("保存处理信息");
        }
        
        // 设置处理人
        if (alarm.getProcessInfo() != null && !alarm.getProcessInfo().isEmpty()) {
            tvProcessor.setText("处理信息: " + alarm.getProcessInfo());
            tvProcessor.setVisibility(View.VISIBLE);
        } else {
            tvProcessor.setVisibility(View.GONE);
        }
        
        // 设置处理时间
        if (alarm.getSolve_time() != null && !alarm.getSolve_time().isEmpty()) {
            String processTimeStr = alarm.getSolve_time();
            tvProcessTime.setText("处理时间: " + processTimeStr);
            tvProcessTime.setVisibility(View.VISIBLE);
        } else {
            tvProcessTime.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProcessInfo());
        btnExport.setOnClickListener(v -> exportImage());
        btnClose.setOnClickListener(v -> dismiss());
        
        // 点击图片查看大图
        imageView.setOnClickListener(v -> {
            // 这里可以实现图片放大查看功能
            Toast.makeText(getContext(), "图片查看功能", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProcessInfo() {
        String processInfo = etProcessInfo.getText().toString().trim();
        if (processInfo.isEmpty()) {
            Toast.makeText(getContext(), "请输入处理信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新报警信息
        alarm.setProcessInfo("当前用户: " + etProcessInfo.getText().toString()); // 设置处理信息，包含用户名和处理内容
        alarm.setProcessInfo("当前用户"); // 使用setProcessInfo替代不存在的setProcessor
        alarm.setSolve_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()))); // 使用setSolve_time替代不存在的setProcessTime
        alarm.setStatus(AlarmItem.STATUS_PROCESSED); // 使用常量替代字符串
        
        // 刷新显示
        setupData();
        
        Toast.makeText(getContext(), "处理信息已保存", Toast.LENGTH_SHORT).show();
    }

    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private void exportImage() {
        // 检查并请求存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasStoragePermission()) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
            return;
        }
    
        try {
            String path = alarm.getPath();
            if (path != null && !path.isEmpty()) {
                // 构建完整的图片URL用于导出
                String imageUrl = "http://" + globalIP + ":5004/data/media/" + path;
                
                DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                String fileName = "alarm_" + alarm.getId() + "_" + System.currentTimeMillis() + ".jpg";
                
                // 优化DownloadManager请求配置
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
                request.setTitle("报警图片导出");
                request.setDescription("导出报警图片到本地");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);
                request.setAllowedOverMetered(true);  // 允许在计量网络下下载
                request.setAllowedOverRoaming(true);  // 允许在漫游网络下下载
                request.setMimeType("image/jpeg");   // 设置MIME类型
                
                // 开始下载
                long downloadId = downloadManager.enqueue(request);
                Toast.makeText(getContext(), "图片导出中，请查看通知栏", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "没有可导出的图片", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("AlarmDetailDialog", "导出图片失败", e);
            Toast.makeText(getContext(), "图片导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // 检查是否有存储权限
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上版本
            return Environment.isExternalStorageManager();
        } else {
            // Android 10及以下版本
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                     @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，再次尝试导出
                exportImage();
            } else {
                Toast.makeText(getContext(), "需要存储权限才能导出图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // 设置对话框宽度为屏幕宽度的90%
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}