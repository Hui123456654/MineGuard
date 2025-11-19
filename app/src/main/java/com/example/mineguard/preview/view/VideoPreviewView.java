package com.example.mineguard.preview.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.mineguard.R;
import com.example.mineguard.preview.model.DeviceItem;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 视频预览组件
 */
public class VideoPreviewView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private static final String TAG = "VideoPreviewView";

    private TextureView textureView;
    private ImageView ivPlaceholder;
    private TextView tvDeviceName;
    private TextView tvDeviceStatus;
    private View statusIndicator;

    private VLCVideoLayout vlcVideoLayout;
    private LibVLC libVLC;
    private MediaPlayer vlcPlayer;
    private DeviceItem currentDevice;
    private boolean isPrepared = false;

    public interface OnPreviewClickListener {
        void onPreviewClick(DeviceItem device);
        void onPreviewLongClick(DeviceItem device);
    }

    private OnPreviewClickListener clickListener;

    public VideoPreviewView(@NonNull Context context) {
        super(context);
        initView();
    }

    public VideoPreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VideoPreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.view_video_preview, this);

        textureView = findViewById(R.id.texture_view);
        ivPlaceholder = findViewById(R.id.iv_placeholder);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        statusIndicator = findViewById(R.id.status_indicator);

        vlcVideoLayout = new VLCVideoLayout(getContext());
        vlcVideoLayout.setVisibility(GONE);
        addView(vlcVideoLayout, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        textureView.setSurfaceTextureListener(this);

        setOnClickListener(v -> {
            if (clickListener != null && currentDevice != null) {
                clickListener.onPreviewClick(currentDevice);
            }
        });

        setOnLongClickListener(v -> {
            if (clickListener != null && currentDevice != null) {
                clickListener.onPreviewLongClick(currentDevice);
            }
            return true;
        });
    }

    public void setDevice(DeviceItem device) {
        Log.d(TAG, "setDevice called for: " + (device != null ? device.getName() : "null"));
        this.currentDevice = device;
        updateDeviceInfo();

        if (device != null && device.isOnline() && device.getVideoUrl() != null) {
            Log.d(TAG, "Device is online with a video URL. Starting playback.");
            stopVideoPlayback();
            initializeRTSP(device.getVideoUrl());
        } else {
            Log.d(TAG, "Device is offline or has no video URL. Stopping playback.");
            stopVideoPlayback();
        }
    }

    private void updateDeviceInfo() {
        if (currentDevice == null) {
            tvDeviceName.setText("");
            tvDeviceStatus.setText("");
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.text_secondary));
            return;
        }

        tvDeviceName.setText(currentDevice.getName());
        tvDeviceStatus.setText(currentDevice.getStatusName());

        switch (currentDevice.getStatus()) {
            case DeviceItem.STATUS_ONLINE:
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.primary_green));
                break;
            case DeviceItem.STATUS_OFFLINE:
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.text_secondary));
                break;
            case DeviceItem.STATUS_ERROR:
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.primary_red));
                break;
        }
    }

    private void initializeRTSP(String videoUrl) {
        Log.d(TAG, "initializeRTSP with URL: " + videoUrl);
        try {
            Log.e(TAG, "VLC-try-Error", new Throwable("Info"));
            String rtspUrl = videoUrl;
            Log.d(TAG, "RTSPURL地址是: " + rtspUrl);

            ArrayList<String> options = new ArrayList<>();
            options.add("--rtsp-tcp");
            options.add("--network-caching=10000");
            options.add("--avcodec-hw=any");

            vlcVideoLayout.setVisibility(VISIBLE);
            ivPlaceholder.setVisibility(GONE);

            libVLC = new LibVLC(getContext(), options);
            vlcPlayer = new MediaPlayer(libVLC);

            Uri uri = Uri.parse(rtspUrl);
            Media media = new Media(libVLC, uri);
            vlcPlayer.setMedia(media);

            vlcPlayer.attachViews(vlcVideoLayout, null, false, false);
            vlcPlayer.play();

            isPrepared = true;
            textureView.setVisibility(GONE);
            Log.d(TAG, "LibVLC MediaPlayer started. RTSP should be playing.");
        } catch (Exception e) {
            Log.e(TAG, "VLC---Error Error initializing RTSP playback: " + e.getMessage());
            e.printStackTrace();
            if (vlcPlayer != null) {
                try { vlcPlayer.stop(); } catch (Exception ignore) {}
                try { vlcPlayer.detachViews(); } catch (Exception ignore) {}
            }
            isPrepared = false;
            ivPlaceholder.setVisibility(VISIBLE);
            vlcVideoLayout.setVisibility(GONE);
            textureView.setVisibility(GONE);
        }
    }

    private void stopVideoPlayback() {
        Log.d(TAG, "stopVideoPlayback called.");
        if (vlcPlayer != null) {
            try {
                if (vlcPlayer.isPlaying()) {
                    vlcPlayer.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping VLC MediaPlayer", e);
            }
            try {
                vlcPlayer.detachViews();
            } catch (Exception e) {
                Log.e(TAG, "Error detaching VLC views", e);
            }
            try {
                vlcPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing VLC MediaPlayer", e);
            }
            vlcPlayer = null;
        }
        if (libVLC != null) {
            try {
                libVLC.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing LibVLC", e);
            }
            libVLC = null;
        }
        isPrepared = false;
        ivPlaceholder.setVisibility(VISIBLE);
        vlcVideoLayout.setVisibility(GONE);
        textureView.setVisibility(GONE);
    }

    public void setOnPreviewClickListener(OnPreviewClickListener listener) {
        this.clickListener = listener;
    }

    public DeviceItem getCurrentDevice() {
        return currentDevice;
    }

    public boolean isPlaying() {
        return vlcPlayer != null && vlcPlayer.isPlaying();
    }

    public void pausePlayback() {
        Log.d(TAG, "pausePlayback called.");
        if (vlcPlayer != null && vlcPlayer.isPlaying()) {
            vlcPlayer.pause();
        }
    }

    public void resumePlayback() {
        Log.d(TAG, "resumePlayback called.");
        if (vlcPlayer != null && isPrepared && !vlcPlayer.isPlaying()) {
            vlcPlayer.play();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: Surface Texture is now available.");
        // 使用 LibVLC 渲染到 VLCVideoLayout，TextureView 不参与渲染。
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureDestroyed: Surface Texture is destroyed.");
        stopVideoPlayback();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        // 这个回调非常频繁，通常不需要日志
    }

    public void release() {
        Log.d(TAG, "release called. Releasing all resources.");
        stopVideoPlayback();
        if (vlcPlayer != null) {
            try { vlcPlayer.release(); } catch (Exception ignore) {}
            vlcPlayer = null;
        }
        if (libVLC != null) {
            try { libVLC.release(); } catch (Exception ignore) {}
            libVLC = null;
        }
    }

    // 使用 LibVLC 播放，不再需要 Android MediaPlayer 错误解析方法。

    // 使用 LibVLC 播放，不再需要 Android MediaPlayer 错误解析方法。
}
