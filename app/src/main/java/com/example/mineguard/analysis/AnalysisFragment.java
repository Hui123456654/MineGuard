package com.example.mineguard.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem; // 新增导入
import androidx.media3.exoplayer.ExoPlayer; // 新增导入
import androidx.media3.ui.PlayerView; // 新增导入

// 👇 补上这几行
import android.view.SurfaceView;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.MediaSource;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;

import java.util.ArrayList;
import java.util.List;

public class AnalysisFragment extends Fragment {

    private View grid1View;
    private View grid4View;

    // 1. 定义播放器变量
    private PlayerView playerView;
    private ExoPlayer player;
    // 替换成你真实的 RTSP 地址
    private String rtspUrl = "rtsp://192.168.31.64/live/raw";

    // === 新增：四宫格相关变量 ===
    private SurfaceView[] gridSurfaceViews = new SurfaceView[4]; // 存放 XML 里的 sv_cam_01 等
    private ExoPlayer[] gridPlayers = new ExoPlayer[4];          // 存放 4 个播放器实例
    // 模拟 4 个摄像头的地址 (目前先都用同一个测试，以后你可以换成不同的)
    private String[] gridUrls;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2. 绑定 PlayerView 控件
        // 注意：这里要对应你在 XML 修改后的 ID
        playerView = view.findViewById(R.id.player_view_main);

        // === 新增：绑定 4 个 SurfaceView ===
        gridSurfaceViews[0] = view.findViewById(R.id.sv_cam_01);
        gridSurfaceViews[1] = view.findViewById(R.id.sv_cam_02);
        gridSurfaceViews[2] = view.findViewById(R.id.sv_cam_03);
        gridSurfaceViews[3] = view.findViewById(R.id.sv_cam_04);
        // 初始化 4 个地址 (这里为了测试，我全部填了一样的)
        gridUrls = new String[] { rtspUrl, rtspUrl, rtspUrl, rtspUrl };

        // --- 原有的逻辑保持不变 ---
        grid1View = view.findViewById(R.id.grid_1_view);
        grid4View = view.findViewById(R.id.grid_4_view);

        RecyclerView rvDeviceList = view.findViewById(R.id.rv_device_list);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> mockDevices = new ArrayList<>();
        mockDevices.add("主井皮带机CAM01");
        mockDevices.add("破碎站入口CAM02");
        mockDevices.add("通风井口CAM03");
        mockDevices.add("变电所CAM04");

        SimpleDeviceAdapter deviceAdapter = new SimpleDeviceAdapter(mockDevices);
        rvDeviceList.setAdapter(deviceAdapter);

        RecyclerView rvAlarmList = view.findViewById(R.id.rv_alarm_list);
        rvAlarmList.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> mockAlarms = new ArrayList<>();
        mockAlarms.add("皮带跑偏告警 #1");
        mockAlarms.add("人员入侵检测 #2");
        mockAlarms.add("温度异常升高 #3");

        AlarmAdapter alarmAdapter = new AlarmAdapter(mockAlarms);
        rvAlarmList.setAdapter(alarmAdapter);

        ImageButton btnGrid1 = view.findViewById(R.id.btn_grid_1);
        ImageButton btnGrid4 = view.findViewById(R.id.btn_grid_4);
        Button btnDisarm = view.findViewById(R.id.btn_disarm);
        Button btnClose = view.findViewById(R.id.btn_close);
        Button btnIntercom = view.findViewById(R.id.btn_intercom);

        btnGrid1.setOnClickListener(v -> {
            grid1View.setVisibility(View.VISIBLE);
            grid4View.setVisibility(View.GONE);
            // 1. 启动大屏
            initializePlayer();
            if (player != null) player.play();

            // 2. 暂停/释放四宫格 (节省资源)
            stopGridPlayers();
            Toast.makeText(getContext(), "切换至单路视频", Toast.LENGTH_SHORT).show();
        });

        btnGrid4.setOnClickListener(v -> {
            grid1View.setVisibility(View.GONE);
            grid4View.setVisibility(View.VISIBLE);
            // 切换到四宫格时，可以暂停大屏播放节省资源
            if (player != null) {
                player.pause();
            }
            initGridPlayers();
            Toast.makeText(getContext(), "切换至四路视频", Toast.LENGTH_SHORT).show();
        });

        btnDisarm.setOnClickListener(v -> Toast.makeText(getContext(), "撤防指令已发送", Toast.LENGTH_SHORT).show());
        btnClose.setOnClickListener(v -> Toast.makeText(getContext(), "关闭操作", Toast.LENGTH_SHORT).show());
        btnIntercom.setOnClickListener(v -> Toast.makeText(getContext(), "开启对讲", Toast.LENGTH_SHORT).show());
    }

    // 3. 编写初始化播放器的方法
    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(requireContext()).build();
            playerView.setPlayer(player);

            // === 新增调试代码 开始 ===
            player.addListener(new androidx.media3.common.Player.Listener() {
                @Override
                public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                    // 错误会在这里打印出来！
                    // 如果是 Source Error，说明连不上或者没加 RTSP 包
                    // 如果是 Decoder Error，说明模拟器不支持这个视频编码
                    android.util.Log.e("RTSP_DEBUG", "播放失败: " + error.getMessage(), error);
                    Toast.makeText(getContext(), "播放出错: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    String stateString;
                    switch (playbackState) {
                        case androidx.media3.common.Player.STATE_IDLE: stateString = "空闲"; break;
                        case androidx.media3.common.Player.STATE_BUFFERING: stateString = "缓冲中..."; break;
                        case androidx.media3.common.Player.STATE_READY: stateString = "准备就绪"; break;
                        case androidx.media3.common.Player.STATE_ENDED: stateString = "播放结束"; break;
                        default: stateString = "未知"; break;
                    }
                    android.util.Log.d("RTSP_DEBUG", "当前状态: " + stateString);
                }
            });
            // === 新增调试代码 结束 ===
            // 设置 RTSP 媒体源
            MediaItem mediaItem = MediaItem.fromUri(rtspUrl);
            player.setMediaItem(mediaItem);
            player.prepare();
        }
        // 如果当前是单路视图模式，则自动播放
        if (grid1View.getVisibility() == View.VISIBLE) {
            player.play();
        }
    }


    // === 新增：初始化四路播放器 ===
    private void initGridPlayers() {
        for (int i = 0; i < 4; i++) {
            // 如果播放器还没创建，就创建它
            if (gridPlayers[i] == null) {
                ExoPlayer.Builder builder = new ExoPlayer.Builder(requireContext());
                gridPlayers[i] = builder.build();

                // 【关键】把播放器画面输出到对应的 SurfaceView 上
                gridPlayers[i].setVideoSurfaceView(gridSurfaceViews[i]);

                // 设置静音 (4个声音一起放会很吵)
                gridPlayers[i].setVolume(0f);
            }

            // 如果没有正在播放，就开始加载资源
            if (!gridPlayers[i].isPlaying()) {
                MediaItem mediaItem = MediaItem.fromUri(gridUrls[i]);
                // 依然使用 TCP 模式防止花屏
                MediaSource mediaSource = new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(mediaItem);

                gridPlayers[i].setMediaSource(mediaSource);
                gridPlayers[i].prepare();
                gridPlayers[i].play();
            }
        }
    }

    // === 新增：停止四路播放器 ===
    private void stopGridPlayers() {
        for (int i = 0; i < 4; i++) {
            if (gridPlayers[i] != null) {
                gridPlayers[i].stop();
                // 注意：这里可以选择 release() 彻底销毁，也可以只 stop()
                // 为了内存考虑，建议切走时彻底销毁，切回来时重建
                gridPlayers[i].release();
                gridPlayers[i] = null;
            }
        }
    }
    // 4. 编写释放播放器的方法
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    // 5. 处理 Fragment 生命周期 (非常重要！)
    @Override
    public void onStart() {
        super.onStart();
        // 当页面可见时，初始化播放器
        initializePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        // 当页面不可见（比如切到后台或换页面）时，释放资源
        releasePlayer();
        stopGridPlayers(); // 释放四宫格
    }
}