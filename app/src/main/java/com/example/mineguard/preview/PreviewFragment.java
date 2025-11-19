package com.example.mineguard.preview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mineguard.R;
import com.example.mineguard.preview.adapter.DeviceAdapter;
import com.example.mineguard.preview.model.DeviceItem;
import com.example.mineguard.preview.view.ControlButtonsView;
import com.example.mineguard.preview.view.SplitScreenManager;
import com.example.mineguard.preview.view.VideoPreviewView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.ContentValues.TAG;

/**
 * 实时预览Fragment
 * 实现设备列表展示、视频预览、分屏功能、截图录像对讲等功能
 */
public class PreviewFragment extends Fragment implements
        DeviceAdapter.OnDeviceClickListener,
        SplitScreenManager.OnSplitScreenChangeListener,
        ControlButtonsView.OnControlClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;//没用上

    // UI组件
    private RecyclerView recyclerViewDevices;
    private LinearLayout layoutDeviceList;
    private FrameLayout layoutVideoPreview;
    private LinearLayout layoutEmpty;
    private LinearLayout splitModeSelector;
    private LinearLayout videoSwitchControls;
    private ViewGroup videoContainer;
    private ControlButtonsView controlButtons;
    private TextView tvStatusInfo;
    private TextView tvDeviceCount;
    private TextView tvBottomDeviceCount;
    private TextView tvPageInfo;
    private Button btnSplitMode;
    private Button btnRefresh;
    private Button btnPreviousPage;
    private Button btnNextPage;

    // 适配器和管理器
    private DeviceAdapter deviceAdapter;
    private SplitScreenManager splitScreenManager;

    // 数据
    private List<DeviceItem> deviceList;
    private boolean isVideoMode = false;
    private DeviceItem currentSelectedDevice;

    // Handler用于定时更新
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    public PreviewFragment() {
        // Required empty public constructor
    }

    public static PreviewFragment newInstance(String param1, String param2) {
        PreviewFragment fragment = new PreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupSplitScreenManager();
        setupClickListeners();
        loadDeviceData();
        startPeriodicUpdate();
        splitScreenManager.setSplitMode(SplitScreenManager.MODE_QUAD); // <--- 添加这一行，设置一个默认的四分屏模式
        return view;
    }

    private void initViews(View view) {
        recyclerViewDevices = view.findViewById(R.id.recyclerView_devices);
        layoutDeviceList = view.findViewById(R.id.layout_device_list);
        layoutVideoPreview = view.findViewById(R.id.layout_video_preview);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        splitModeSelector = view.findViewById(R.id.split_mode_selector);
        videoSwitchControls = view.findViewById(R.id.video_switch_controls);
        videoContainer = view.findViewById(R.id.video_container);
        controlButtons = view.findViewById(R.id.control_buttons);
        tvStatusInfo = view.findViewById(R.id.tv_status_info);
        tvDeviceCount = view.findViewById(R.id.tv_device_count);
        tvBottomDeviceCount = view.findViewById(R.id.tv_bottom_device_count);
        tvPageInfo = view.findViewById(R.id.tv_page_info);
        btnSplitMode = view.findViewById(R.id.btn_split_mode);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnPreviousPage = view.findViewById(R.id.btn_previous_page);
        btnNextPage = view.findViewById(R.id.btn_next_page);

        // 分屏模式按钮
        view.findViewById(R.id.btn_mode_single).setOnClickListener(v -> {
            splitScreenManager.setSplitMode(SplitScreenManager.MODE_SINGLE);
            hideSplitModeSelector();
        });

        view.findViewById(R.id.btn_mode_quad).setOnClickListener(v -> {
            splitScreenManager.setSplitMode(SplitScreenManager.MODE_QUAD);
            hideSplitModeSelector();
        });

        view.findViewById(R.id.btn_mode_nine).setOnClickListener(v -> {
            splitScreenManager.setSplitMode(SplitScreenManager.MODE_NINE);
            hideSplitModeSelector();
        });

        view.findViewById(R.id.btn_mode_sixteen).setOnClickListener(v -> {
            splitScreenManager.setSplitMode(SplitScreenManager.MODE_SIXTEEN);
            hideSplitModeSelector();
        });
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(this);
        
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void setupSplitScreenManager() {
        splitScreenManager = new SplitScreenManager(requireContext(), 
                getChildFragmentManager(), videoContainer);
        splitScreenManager.setOnSplitScreenChangeListener(this);
    }

    private void setupClickListeners() {
        btnSplitMode.setOnClickListener(v -> {
            if (isVideoMode) {
                toggleSplitModeSelector();
            } else {
                // 切换到视频预览模式
                Log.d(TAG, "setupClickListeners: 切换到视频预览模式 ");
                switchToVideoMode();
            }
        });

        btnRefresh.setOnClickListener(v -> {
            loadDeviceData();
            Toast.makeText(requireContext(), "设备列表已刷新", Toast.LENGTH_SHORT).show();
        });

        // 视频切换按钮事件
        btnPreviousPage.setOnClickListener(v -> {
            if (splitScreenManager.previousPage()) {
                updatePageInfo();
                Toast.makeText(requireContext(), "切换到上一页", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "已经是第一页", Toast.LENGTH_SHORT).show();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (splitScreenManager.nextPage()) {
                updatePageInfo();
                Toast.makeText(requireContext(), "切换到下一页", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDeviceData() {
        generateMockDevices();
        deviceAdapter.updateDevices(deviceList);
        updateDeviceCount();
        updateEmptyState();
    }

    private void generateMockDevices() {
        deviceList.clear();
        Random random = new Random();

        // ... (其他设备列表的添加代码保持不变) ...
        deviceList.add(new DeviceItem("CAM_001", "摄像头001", "掘进工作面", "生产区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_002", "摄像头002", "采煤工作面", "生产区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_003", "摄像头003", "仓库A区", "仓储区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_OFFLINE));
        deviceList.add(new DeviceItem("CAM_004", "摄像头004", "仓库B区", "仓储区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_005", "摄像头005", "办公大厅", "办公区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_006", "摄像头006", "会议室", "办公区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_007", "摄像头007", "主入口", "公共区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));
        deviceList.add(new DeviceItem("CAM_008", "摄像头008", "停车场", "公共区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ERROR));
        deviceList.add(new DeviceItem("CAM_009", "摄像头009", "危险品仓库", "危险区域",
                DeviceItem.TYPE_CAMERA, DeviceItem.STATUS_ONLINE));

        // --- 修改部分 ---
        // 使用资源ID (R.raw.sample_video) 来构建路径，而不是字符串 "/raw/sample_video.mp4"
        String videoPath = "android.resource://" + requireContext().getPackageName() + "/" + R.raw.sample_video;

        // 设置模拟视频URL
        for (DeviceItem device : deviceList) {
            if (device.getType() == DeviceItem.TYPE_CAMERA && device.isOnline()) {
                // 将之前错误的行替换为下面这行
                device.setVideoUrl(videoPath);
            }
        }
    }

    private void updateDeviceCount() {
        int totalCount = deviceList.size();
        int onlineCount = 0;
        for (DeviceItem device : deviceList) {
            if (device.isOnline()) {
                onlineCount++;
            }
        }
        String countText = onlineCount + "/" + totalCount + " 在线";
        tvDeviceCount.setText(countText);
        tvBottomDeviceCount.setText(countText);
    }
    private void updateEmptyState() {
        if (deviceList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            layoutDeviceList.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            layoutDeviceList.setVisibility(View.VISIBLE);
        }
    }

    private void switchToVideoMode() {
        isVideoMode = true;
        layoutDeviceList.setVisibility(View.GONE);
        layoutVideoPreview.setVisibility(View.VISIBLE);
        btnSplitMode.setText("分屏模式");
        tvStatusInfo.setText("视频预览模式");

        // 设置默认设备到分屏管理器
        List<DeviceItem> onlineDevices = new ArrayList<>();
        for (DeviceItem device : deviceList) {
            if (device.isOnline() && device.getType() == DeviceItem.TYPE_CAMERA) {
                onlineDevices.add(device);
                if (onlineDevices.size() >= 4) break; // 默认显示4个
            }
        }

        if (!onlineDevices.isEmpty()) {
            splitScreenManager.setAllDevices(onlineDevices);
            updatePageInfo();
            showVideoSwitchControls();
        }
    }

    private void switchToDeviceList() {
        isVideoMode = false;
        layoutDeviceList.setVisibility(View.VISIBLE);
        layoutVideoPreview.setVisibility(View.GONE);
        btnSplitMode.setText("视频预览");
        tvStatusInfo.setText("设备列表");
        controlButtons.hideControls();
        splitModeSelector.setVisibility(View.GONE);
        hideVideoSwitchControls();
    }

    private void showVideoSwitchControls() {
        // 只在四分屏模式下显示切换按钮
        if (splitScreenManager.getCurrentMode() == SplitScreenManager.MODE_QUAD) {
            videoSwitchControls.setVisibility(View.VISIBLE);
        } else {
            videoSwitchControls.setVisibility(View.GONE);
        }
    }

    private void hideVideoSwitchControls() {
        videoSwitchControls.setVisibility(View.GONE);
    }

    private void updatePageInfo() {
        int currentPage = splitScreenManager.getCurrentPage() + 1; // 页码从1开始显示
        int totalPages = splitScreenManager.getTotalPages();
        tvPageInfo.setText(currentPage + "/" + totalPages);
    }

    private void toggleSplitModeSelector() {
        if (splitModeSelector.getVisibility() == View.VISIBLE) {
            splitModeSelector.setVisibility(View.GONE);
        } else {
            splitModeSelector.setVisibility(View.VISIBLE);
        }
    }

    private void hideSplitModeSelector() {
        splitModeSelector.setVisibility(View.GONE);
    }

    private void startPeriodicUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // 模拟设备状态更新
                updateDeviceStatus();
                handler.postDelayed(this, 30000); // 每30秒更新一次
            }
        };
        handler.post(updateRunnable);
    }

    private void updateDeviceStatus() {
        Random random = new Random();
        for (DeviceItem device : deviceList) {
            // 随机更新一些设备状态
            if (random.nextInt(10) == 0) {
                int newStatus = random.nextInt(3) + 1;
                device.setStatus(newStatus);
                device.setLastUpdateTime(System.currentTimeMillis());
            }
        }
        
        if (deviceAdapter != null) {
            deviceAdapter.updateDevices(deviceList);
        }
        
        if (splitScreenManager != null && isVideoMode) {
            // 更新分屏管理器中的设备状态
            for (int i = 0; i < splitScreenManager.getVisibleDeviceCount(); i++) {
                DeviceItem device = splitScreenManager.getDeviceAt(i);
                if (device != null) {
                    for (DeviceItem updatedDevice : deviceList) {
                        if (updatedDevice.getId().equals(device.getId())) {
                            splitScreenManager.updateDevice(updatedDevice);
                            break;
                        }
                    }
                }
            }
        }
        
        updateDeviceCount();
    }

    // DeviceAdapter.OnDeviceClickListener 实现
    @Override
    public void onDeviceClick(DeviceItem device) {
        currentSelectedDevice = device;
        if (device.getType() == DeviceItem.TYPE_CAMERA && device.isOnline()) {
            switchToVideoMode();
            // 只显示选中的设备
            List<DeviceItem> singleDevice = new ArrayList<>();
            singleDevice.add(device);
            splitScreenManager.setDevices(singleDevice);
            splitScreenManager.setSplitMode(SplitScreenManager.MODE_SINGLE);
        } else {
            Toast.makeText(requireContext(), "该设备不支持视频预览", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeviceLongClick(DeviceItem device) {
        // 长按显示设备详情
        Toast.makeText(requireContext(), "设备: " + device.getName() + 
                "\n状态: " + device.getStatusName() + 
                "\n位置: " + device.getArea(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onQuickViewClick(String region, List<DeviceItem> devices) {
        // 过滤出在线的摄像头设备
        List<DeviceItem> onlineDevices = new ArrayList<>();
        for (DeviceItem device : devices) {
            if (device.isOnline() && device.getType() == DeviceItem.TYPE_CAMERA) {
                onlineDevices.add(device);
            }
        }
        
        if (onlineDevices.isEmpty()) {
            Toast.makeText(requireContext(), "该区域没有在线的摄像头设备", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 切换到视频预览模式
        switchToVideoMode();
        
        // 设置四分屏模式，显示前4个在线设备
        List<DeviceItem> displayDevices = new ArrayList<>();
        for (int i = 0; i < Math.min(4, onlineDevices.size()); i++) {
            displayDevices.add(onlineDevices.get(i));
        }
        
        splitScreenManager.setAllDevices(onlineDevices);
        splitScreenManager.setSplitMode(SplitScreenManager.MODE_QUAD);
        updatePageInfo();
        showVideoSwitchControls();
        
        Toast.makeText(requireContext(), "正在查看 " + region + " 的摄像头", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onModeChanged(int newMode) {
        String modeText = "";
        switch (newMode) {
            case SplitScreenManager.MODE_SINGLE:
                modeText = "单画面";
                break;
            case SplitScreenManager.MODE_QUAD:
                modeText = "四画面";
                break;
            case SplitScreenManager.MODE_NINE:
                modeText = "九画面";
                break;
            case SplitScreenManager.MODE_SIXTEEN:
                modeText = "十六画面";
                break;
        }
        Toast.makeText(requireContext(), "切换到" + modeText + "模式", Toast.LENGTH_SHORT).show();

        showVideoSwitchControls();
    }

    @Override
    public void onPreviewClick(DeviceItem device, int position) {
        currentSelectedDevice = device;
        controlButtons.setDevice(device);
        controlButtons.showControls();
    }

    @Override
    public void onPreviewLongClick(DeviceItem device, int position) {
        Toast.makeText(requireContext(), "设备: " + device.getName() + 
                "\n状态: " + device.getStatusName(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onScreenshotClick(DeviceItem device) {
        Toast.makeText(requireContext(), "截图: " + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordClick(DeviceItem device, boolean start) {
        String message = start ? "开始录像: " + device.getName() : "停止录像: " + device.getName();
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        device.setRecording(start);
    }

    @Override
    public void onTalkClick(DeviceItem device, boolean start) {
        String message = start ? "开始对讲: " + device.getName() : "停止对讲: " + device.getName();
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        device.setTalking(start);
    }

    @Override
    public void onFullscreenClick(DeviceItem device) {
        Toast.makeText(requireContext(), "全屏: " + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCloseClick(DeviceItem device) {
        controlButtons.hideControls();
        switchToDeviceList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (splitScreenManager != null) {
            splitScreenManager.release();
        }
    }
}
