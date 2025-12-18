package com.example.mineguard.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.data.DeviceItem; // 导入 DeviceItem
import com.example.mineguard.data.DeviceViewModel;

import java.util.ArrayList;
import java.util.List;

public class AnalysisFragment extends Fragment implements MainActivity.OnAlarmReceivedListener {

    private View grid1View;
    private View grid2View;
    private View grid4View;

    // 单路播放器
    private PlayerView playerView;
    private ExoPlayer player;

    // 【新增】二路播放器
    private TextureView[] grid2TextureViews = new TextureView[2];
    private ExoPlayer[] grid2Players = new ExoPlayer[2];

    // 四路播放器
    private TextureView[] grid4TextureViews = new TextureView[4];
    private ExoPlayer[] grid4Players = new ExoPlayer[4];

    private DeviceViewModel deviceViewModel;
    private SimpleDeviceAdapter deviceAdapter;

    // === 核心修改 1：保存当前设备列表 ===
    private List<DeviceItem> currentDeviceList = new ArrayList<>();

    private RecyclerView rvAlarmList;
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> displayAlarmList = new ArrayList<>();
    // 【新增】提示文字 TextView
    private TextView tvNoAlarmData;

    private boolean isLeftExpanded = true;
    private boolean isRightExpanded = true;
    private androidx.constraintlayout.widget.Guideline guidelineLeft;
    private androidx.constraintlayout.widget.Guideline guidelineRight;
    private View groupLeftPanel;
    private View groupRightPanel;
    private ImageButton btnToggleLeft, btnToggleRight;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        guidelineLeft = view.findViewById(R.id.guideline_left);
        guidelineRight = view.findViewById(R.id.guideline_right);
        groupLeftPanel = view.findViewById(R.id.group_left_panel);
        groupRightPanel = view.findViewById(R.id.group_right_panel);
        btnToggleLeft = view.findViewById(R.id.btn_toggle_left);
        btnToggleRight = view.findViewById(R.id.btn_toggle_right);

        // 左侧切换逻辑
        btnToggleLeft.setOnClickListener(v -> {
            isLeftExpanded = !isLeftExpanded;
            toggleLeftSide();
        });

        // 右侧切换逻辑
        btnToggleRight.setOnClickListener(v -> {
            isRightExpanded = !isRightExpanded;
            toggleRightSide();
        });

        // 初始化视图
        playerView = view.findViewById(R.id.player_view_main);
        // 二路
        grid2TextureViews[0] = view.findViewById(R.id.tv_cam_2_01);
        grid2TextureViews[1] = view.findViewById(R.id.tv_cam_2_02);
        // 四路
        grid4TextureViews[0] = view.findViewById(R.id.tv_cam_01);
        grid4TextureViews[1] = view.findViewById(R.id.tv_cam_02);
        grid4TextureViews[2] = view.findViewById(R.id.tv_cam_03);
        grid4TextureViews[3] = view.findViewById(R.id.tv_cam_04);

        grid1View = view.findViewById(R.id.grid_1_view);
        grid2View = view.findViewById(R.id.grid_2_view);
        grid4View = view.findViewById(R.id.grid_4_view);

        // 初始化设备列表
        deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        RecyclerView rvDeviceList = view.findViewById(R.id.rv_device_list);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new SimpleDeviceAdapter(new ArrayList<>());
        rvDeviceList.setAdapter(deviceAdapter);

        // === 核心修改 2：监听数据变化并自动刷新视频 ===
        deviceViewModel.getLiveDeviceList().observe(getViewLifecycleOwner(), deviceItems -> {
            // 1. 更新 Adapter 显示
            deviceAdapter.setDeviceList(deviceItems);

            // 2. 更新本地缓存的列表
            currentDeviceList.clear();
            if (deviceItems != null) {
                currentDeviceList.addAll(deviceItems);
            }

            // 3. 数据来了，立即刷新当前显示的视频
            refreshCurrentVideoMode();
        });

        // 初始化报警列表
        rvAlarmList = view.findViewById(R.id.rv_alarm_list);
        rvAlarmList.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmAdapter = new AlarmAdapter(displayAlarmList);
        rvAlarmList.setAdapter(alarmAdapter);
        // 【新增】初始化提示文字
        tvNoAlarmData = view.findViewById(R.id.tv_no_alarm_data);
        setupClickListeners(view);
    }

    private void toggleLeftSide() {
        // 开启布局动画
        androidx.transition.TransitionManager.beginDelayedTransition((ViewGroup) getView());

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) guidelineLeft.getLayoutParams();

        if (isLeftExpanded) {
            params.guidePercent = 0.18f;
            groupLeftPanel.setVisibility(View.VISIBLE);
            btnToggleLeft.setImageResource(R.drawable.icon_left); // 指向左，表示可以收起
        } else {
            params.guidePercent = 0.0f;
            groupLeftPanel.setVisibility(View.GONE);
            btnToggleLeft.setImageResource(R.drawable.icon_right); // 指向右，表示可以展开
        }
        guidelineLeft.setLayoutParams(params);
        // ======= 核心优化添加处 =======
        // 这里的 video_container 是你在 xml 中定义的 FrameLayout ID
        guidelineLeft.postDelayed(() -> {
            View videoContainer = getView().findViewById(R.id.video_container);
            if (videoContainer != null) {
                videoContainer.requestLayout();
            }
        }, 350); // 稍微多给 50ms 确保 Transition 动画彻底完成
    }

    private void toggleRightSide() {
        androidx.transition.TransitionManager.beginDelayedTransition((ViewGroup) getView());

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) guidelineRight.getLayoutParams();

        if (isRightExpanded) {
            params.guidePercent = 0.82f;
            groupRightPanel.setVisibility(View.VISIBLE);
            btnToggleRight.setImageResource(R.drawable.icon_right); // 指向右，表示可以收起
        } else {
            params.guidePercent = 1.0f;
            groupRightPanel.setVisibility(View.GONE);
            btnToggleRight.setImageResource(R.drawable.icon_left); // 指向左，表示可以展开
        }
        guidelineRight.setLayoutParams(params);
        // ======= 核心优化添加处 =======
        guidelineRight.postDelayed(() -> {
            View videoContainer = getView().findViewById(R.id.video_container);
            if (videoContainer != null) {
                videoContainer.requestLayout();
            }
        }, 350);
    }

    // === 核心修改 3：统一刷新逻辑 ===
    private void refreshCurrentVideoMode() {
        // 释放所有播放器资源
        releasePlayer(); // 确保单路停止
        stopGrid2Players(); // 确保二路停止
        stopGrid4Players(); // 确保四路停止

        if (grid1View.getVisibility() == View.VISIBLE) {
            // 单路模式
            initializePlayer();
        } else if (grid2View.getVisibility() == View.VISIBLE) {
            // 【新增】二路模式
            initGrid2Players();
        } else { // grid4View.getVisibility() == View.VISIBLE
            // 四路模式
            initGrid4Players();
        }
    }

    // 生命周期部分保持不变
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.addAlarmListener(this);
            loadDataFromActivity(mainActivity);
        }
        // 页面恢复时，恢复视频播放
        refreshCurrentVideoMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).removeAlarmListener(this);
        }
        // 页面不可见时，释放所有播放器资源
        releasePlayer();
        stopGrid2Players();
        stopGrid4Players();
    }

    private void loadDataFromActivity(MainActivity mainActivity) {
        List<AlarmItem> globalList = mainActivity.getGlobalAlarmList();
        if (globalList != null) {
            displayAlarmList.clear();
            displayAlarmList.addAll(globalList);
            alarmAdapter.notifyDataSetChanged();
        }
        // 【新增】根据列表内容判断显示提示文字或 RecyclerView
        if (displayAlarmList.isEmpty()) {
            tvNoAlarmData.setVisibility(View.VISIBLE);
            rvAlarmList.setVisibility(View.GONE);
        } else {
            tvNoAlarmData.setVisibility(View.GONE);
            rvAlarmList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNewAlarm(AlarmItem item) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                displayAlarmList.add(0, item);
                alarmAdapter.notifyItemInserted(0);
                rvAlarmList.scrollToPosition(0);
                // 【新增】确保在收到新报警时隐藏“未查询到报警信息”
                if (tvNoAlarmData.getVisibility() == View.VISIBLE) {
                    tvNoAlarmData.setVisibility(View.GONE);
                    rvAlarmList.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void setupClickListeners(View view) {
        ImageButton btnGrid1 = view.findViewById(R.id.btn_grid_1);
        ImageButton btnGrid2 = view.findViewById(R.id.btn_grid_2);
        ImageButton btnGrid4 = view.findViewById(R.id.btn_grid_4);
        Button btnDisarm = view.findViewById(R.id.btn_disarm);
        Button btnClose = view.findViewById(R.id.btn_close);
        Button btnIntercom = view.findViewById(R.id.btn_intercom);

        btnGrid1.setOnClickListener(v -> {
            grid1View.setVisibility(View.VISIBLE);
            grid2View.setVisibility(View.GONE);
            grid4View.setVisibility(View.GONE);
            refreshCurrentVideoMode(); // 切换模式时刷新
            Toast.makeText(getContext(), "切换至单路视频", Toast.LENGTH_SHORT).show();
        });

        btnGrid2.setOnClickListener(v -> {
            grid1View.setVisibility(View.GONE);
            grid2View.setVisibility(View.VISIBLE);
            grid4View.setVisibility(View.GONE);
            refreshCurrentVideoMode();
            Toast.makeText(getContext(), "切换至二路视频", Toast.LENGTH_SHORT).show();
        });

        btnGrid4.setOnClickListener(v -> {
            grid1View.setVisibility(View.GONE);
            grid2View.setVisibility(View.GONE);
            grid4View.setVisibility(View.VISIBLE);
            refreshCurrentVideoMode(); // 切换模式时刷新
            Toast.makeText(getContext(), "切换至四路视频", Toast.LENGTH_SHORT).show();
        });

        btnDisarm.setOnClickListener(v -> Toast.makeText(getContext(), "撤防指令已发送", Toast.LENGTH_SHORT).show());
        btnClose.setOnClickListener(v -> Toast.makeText(getContext(), "关闭操作", Toast.LENGTH_SHORT).show());
        btnIntercom.setOnClickListener(v -> Toast.makeText(getContext(), "开启对讲", Toast.LENGTH_SHORT).show());
    }

    // === 核心修改 4：单路播放逻辑 (读取 List 第一个) ===
    private void initializePlayer() {
        // 如果没有设备数据，直接返回
        if (currentDeviceList == null || currentDeviceList.isEmpty()) {
            return;
        }

        // 取出第一个设备的 URL
        String rtspUrl = currentDeviceList.get(0).getRtspUrl();
        if (rtspUrl == null || rtspUrl.isEmpty()) return;

        if (player == null) {
            player = new ExoPlayer.Builder(requireContext()).build();
            playerView.setPlayer(player);
        }

        // 不管 Player 是否为空，都重新设置 MediaSource 以确保 URL 是最新的
        MediaItem mediaItem = MediaItem.fromUri(rtspUrl);
        MediaSource mediaSource = new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
    }
    // === 核心修改5：新增二路播放逻辑 (读取 List 前两个) ===
    private void initGrid2Players() {
        if (currentDeviceList == null) return;

        // 只需要遍历 2 个窗口
        for (int i = 0; i < 2; i++) {
            // 如果当前索引超过了设备总数，则停止该窗口
            if (i >= currentDeviceList.size()) {
                if (grid2Players[i] != null) {
                    grid2Players[i].stop();
                    grid2Players[i].clearMediaItems();
                }
                continue;
            }

            // 获取对应索引的设备 URL
            String url = currentDeviceList.get(i).getRtspUrl();
            if (url == null || url.isEmpty()) continue;

            if (grid2Players[i] == null) {
                ExoPlayer.Builder builder = new ExoPlayer.Builder(requireContext());
                grid2Players[i] = builder.build();
                // 绑定到二路专用的 SurfaceView
                //grid2Players[i].setVideoSurfaceView(grid2SurfaceViews[i]);
                grid2Players[i].setVideoTextureView(grid2TextureViews[i]);
                grid2Players[i].setVolume(0f);
            }

            // 设置播放源
            MediaItem mediaItem = MediaItem.fromUri(url);
            MediaSource mediaSource = new RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .createMediaSource(mediaItem);
            grid2Players[i].setMediaSource(mediaSource);
            grid2Players[i].prepare();
            grid2Players[i].play();
        }
    }

    // ==新增二路停止逻辑 ===
    private void stopGrid2Players() {
        for (int i = 0; i < 2; i++) {
            if (grid2Players[i] != null) {
                grid2Players[i].stop();
                grid2Players[i].release();
                grid2Players[i] = null;
            }
        }
    }

    // === 核心修改 6：四路播放逻辑 (读取 List 前四个) ===
    private void initGrid4Players() {
        if (currentDeviceList == null) return;

        for (int i = 0; i < 4; i++) {
            // 如果当前索引超过了设备总数（例如只有 2 个设备，i=2 时就越界了），则停止该窗口
            if (i >= currentDeviceList.size()) {
                if (grid4Players[i] != null) {
                    grid4Players[i].stop();
                    grid4Players[i].clearMediaItems();
                }
                continue;
            }

            // 获取对应索引的设备 URL
            String url = currentDeviceList.get(i).getRtspUrl();
            if (url == null || url.isEmpty()) continue;

            if (grid4Players[i] == null) {
                ExoPlayer.Builder builder = new ExoPlayer.Builder(requireContext());
                grid4Players[i] = builder.build();
                //grid4Players[i].setVideoSurfaceView(grid4SurfaceViews[i]);
                grid4Players[i].setVideoTextureView(grid4TextureViews[i]);
                grid4Players[i].setVolume(0f);
            }

            // 设置播放源
            MediaItem mediaItem = MediaItem.fromUri(url);
            MediaSource mediaSource = new RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .createMediaSource(mediaItem);
            grid4Players[i].setMediaSource(mediaSource);
            grid4Players[i].prepare();
            grid4Players[i].play();
        }
    }

    private void stopGrid4Players() {
        for (int i = 0; i < 4; i++) {
            if (grid4Players[i] != null) {
                grid4Players[i].stop();
                grid4Players[i].release();
                grid4Players[i] = null;
            }
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    // onStart 和 onStop 通常由 onResume/onPause 覆盖了逻辑，
    // 但为了保险起见，保持简单的 super 调用即可，或者根据需要调用 refresh
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}