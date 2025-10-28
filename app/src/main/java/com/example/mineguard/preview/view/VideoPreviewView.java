package com.example.mineguard.preview.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log; // 导入 Log 类
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.mineguard.R;
import com.example.mineguard.preview.model.DeviceItem;
import java.io.IOException;

/**
 * 视频预览组件
 */
public class VideoPreviewView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private static final String TAG = "VideoPreviewView"; // 用于日志输出的 TAG

    private TextureView textureView;
    private ImageView ivPlaceholder;
    private TextView tvDeviceName;
    private TextView tvDeviceStatus;
    private View statusIndicator;

    private MediaPlayer mediaPlayer;
    private Surface surface;
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
            startVideoPlayback(device.getVideoUrl());
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

    private void startVideoPlayback(String videoUrl) {
        Log.d(TAG, "startVideoPlayback with URL: " + videoUrl);
        if (mediaPlayer == null) {
            Log.d(TAG, "Creating new MediaPlayer instance.");
            mediaPlayer = new MediaPlayer();
        }

        try {
            Log.d(TAG, "Resetting MediaPlayer.");
            mediaPlayer.reset();
            Log.d(TAG, "Setting data source...");
            mediaPlayer.setDataSource(getContext(), Uri.parse(videoUrl));
            mediaPlayer.setLooping(true);

            if (surface != null) {
                Log.d(TAG, "Surface is available. Setting surface for MediaPlayer.");
                mediaPlayer.setSurface(surface);
            } else {
                Log.w(TAG, "Surface is not available yet. Playback will start after surface is created.");
            }

            Log.d(TAG, "Calling prepareAsync().");
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "onPrepared: MediaPlayer is prepared.");
                isPrepared = true;
                Log.d(TAG, "Starting MediaPlayer.");
                mp.start();
                ivPlaceholder.setVisibility(GONE);
                textureView.setVisibility(VISIBLE);
                Log.d(TAG, "MediaPlayer started. Video should be playing.");
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
                ivPlaceholder.setVisibility(VISIBLE);
                textureView.setVisibility(GONE);
                isPrepared = false;
                return true; // 返回 true 表示错误已被处理
            });

        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Error setting data source or preparing MediaPlayer", e);
            e.printStackTrace();
            ivPlaceholder.setVisibility(VISIBLE);
            textureView.setVisibility(GONE);
        }
    }

    private void stopVideoPlayback() {
        Log.d(TAG, "stopVideoPlayback called.");
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                Log.d(TAG, "MediaPlayer stopped and reset.");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping or resetting MediaPlayer", e);
            }
        }
        isPrepared = false;
        ivPlaceholder.setVisibility(VISIBLE);
        textureView.setVisibility(GONE);
    }

    public void setOnPreviewClickListener(OnPreviewClickListener listener) {
        this.clickListener = listener;
    }

    public DeviceItem getCurrentDevice() {
        return currentDevice;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void pausePlayback() {
        Log.d(TAG, "pausePlayback called.");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumePlayback() {
        Log.d(TAG, "resumePlayback called.");
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: Surface Texture is now available.");
        surface = new Surface(surfaceTexture);
        if (mediaPlayer != null) {
            Log.d(TAG, "Setting surface for existing MediaPlayer instance.");
            mediaPlayer.setSurface(surface);
            // 如果之前因为 surface 未准备好而没有播放，可以尝试重新开始
            if (currentDevice != null && currentDevice.getVideoUrl() != null && !isPlaying() && !isPrepared) {
                Log.d(TAG, "Retrying video playback after surface became available.");
                startVideoPlayback(currentDevice.getVideoUrl());
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureDestroyed: Surface Texture is destroyed.");
        if (surface != null) {
            surface.release();
            surface = null;
        }
        // 停止播放，但不释放 MediaPlayer，因为 Activity/Fragment 可能只是暂时不可见
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }
}
