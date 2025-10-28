package com.example.mineguard.preview.view;

import android.content.Context;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentManager;
import com.example.mineguard.preview.model.DeviceItem;
import java.util.ArrayList;
import java.util.List;

/**
 * 分屏管理器，负责管理多画面预览布局
 */
public class SplitScreenManager {
    
    public static final int MODE_SINGLE = 1;  // 单画面
    public static final int MODE_QUAD = 4;    // 四画面
    public static final int MODE_NINE = 9;    // 九画面
    public static final int MODE_SIXTEEN = 16; // 十六画面
    
    private Context context;
    private FragmentManager fragmentManager;
    private ViewGroup container;
    private int currentMode = MODE_SINGLE;
    private List<VideoPreviewView> previewViews;
    private List<DeviceItem> currentDevices;
    private OnSplitScreenChangeListener listener;
    
    public interface OnSplitScreenChangeListener {
        void onModeChanged(int newMode);
        void onPreviewClick(DeviceItem device, int position);
        void onPreviewLongClick(DeviceItem device, int position);
    }
    
    public SplitScreenManager(Context context, FragmentManager fragmentManager, ViewGroup container) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.container = container;
        this.previewViews = new ArrayList<>();
        this.currentDevices = new ArrayList<>();
    }
    
    public void setOnSplitScreenChangeListener(OnSplitScreenChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置分屏模式
     */
    public void setSplitMode(int mode) {
        if (currentMode == mode) {
            return;
        }
        
        currentMode = mode;
        recreateLayout();
        
        if (listener != null) {
            listener.onModeChanged(mode);
        }
    }
    
    /**
     * 获取当前分屏模式
     */
    public int getCurrentMode() {
        return currentMode;
    }
    
    /**
     * 重新创建布局
     */
    private void recreateLayout() {
        // 清理现有视图
        container.removeAllViews();
        previewViews.clear();
        
        // 根据模式创建新的布局
        switch (currentMode) {
            case MODE_SINGLE:
                createSingleLayout();
                break;
            case MODE_QUAD:
                createGridLayout(2, 2);
                break;
            case MODE_NINE:
                createGridLayout(3, 3);
                break;
            case MODE_SIXTEEN:
                createGridLayout(4, 4);
                break;
        }
        
        // 重新设置设备
        if (!currentDevices.isEmpty()) {
            setDevices(currentDevices);
        }
    }
    
    /**
     * 创建单画面布局
     */
    private void createSingleLayout() {
        VideoPreviewView previewView = createPreviewView();
        previewViews.add(previewView);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        container.addView(previewView, params);
    }
    
    /**
     * 创建网格布局
     */
    private void createGridLayout(int rows, int cols) {
        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        gridLayout.setOrientation(GridLayout.VERTICAL);
        
        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
        gridParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        gridParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        gridParams.setMargins(2, 2, 2, 2);
        
        for (int i = 0; i < rows * cols; i++) {
            VideoPreviewView previewView = createPreviewView();
            previewViews.add(previewView);
            
            // 设置布局参数
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % cols, 1f);
            params.rowSpec = GridLayout.spec(i / cols, 1f);
            params.setMargins(2, 2, 2, 2);
            
            gridLayout.addView(previewView, params);
        }
        
        container.addView(gridLayout, gridParams);
    }
    
    /**
     * 创建预览视图
     */
    private VideoPreviewView createPreviewView() {
        VideoPreviewView previewView = new VideoPreviewView(context);
        
        previewView.setOnPreviewClickListener(new VideoPreviewView.OnPreviewClickListener() {
            @Override
            public void onPreviewClick(DeviceItem device) {
                int position = getPreviewPosition(previewView);
                if (listener != null && position >= 0) {
                    listener.onPreviewClick(device, position);
                }
            }
            
            @Override
            public void onPreviewLongClick(DeviceItem device) {
                int position = getPreviewPosition(previewView);
                if (listener != null && position >= 0) {
                    listener.onPreviewLongClick(device, position);
                }
            }
        });
        
        return previewView;
    }
    
    /**
     * 获取预览视图的位置
     */
    private int getPreviewPosition(VideoPreviewView previewView) {
        return previewViews.indexOf(previewView);
    }
    
    /**
     * 设置设备列表
     */
    public void setDevices(List<DeviceItem> devices) {
        Log.d("SplitScreenManager", "setDevices called with " + devices.size() + " devices."); // 添加这行日志
        this.currentDevices = new ArrayList<>(devices);
        updatePreviewViews();
    }
    
    /**
     * 更新预览视图
     */
    private void updatePreviewViews() {
        int maxViews = Math.min(previewViews.size(), currentDevices.size());
        
        for (int i = 0; i < previewViews.size(); i++) {
            if (i < maxViews) {
                DeviceItem device = currentDevices.get(i);
                previewViews.get(i).setDevice(device);
                previewViews.get(i).setVisibility(View.VISIBLE);
                Log.d("SplitScreenManager", "Updating view at index " + i + " for device: " + device.getName()); // 添加这行日志
            } else {
                previewViews.get(i).setDevice(null);
                previewViews.get(i).setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 添加设备
     */
    public void addDevice(DeviceItem device) {
        currentDevices.add(device);
        updatePreviewViews();
    }
    
    /**
     * 移除设备
     */
    public void removeDevice(DeviceItem device) {
        currentDevices.remove(device);
        updatePreviewViews();
    }
    
    /**
     * 更新设备
     */
    public void updateDevice(DeviceItem device) {
        int index = -1;
        for (int i = 0; i < currentDevices.size(); i++) {
            if (currentDevices.get(i).getId().equals(device.getId())) {
                index = i;
                break;
            }
        }
        
        if (index >= 0) {
            currentDevices.set(index, device);
            updatePreviewViews();
        }
    }
    
    /**
     * 获取指定位置的设备
     */
    public DeviceItem getDeviceAt(int position) {
        if (position >= 0 && position < currentDevices.size()) {
            return currentDevices.get(position);
        }
        return null;
    }
    
    /**
     * 获取指定位置的预览视图
     */
    public VideoPreviewView getPreviewViewAt(int position) {
        if (position >= 0 && position < previewViews.size()) {
            return previewViews.get(position);
        }
        return null;
    }
    
    /**
     * 暂停所有预览
     */
    public void pauseAllPreviews() {
        for (VideoPreviewView previewView : previewViews) {
            previewView.pausePlayback();
        }
    }
    
    /**
     * 恢复所有预览
     */
    public void resumeAllPreviews() {
        for (VideoPreviewView previewView : previewViews) {
            previewView.resumePlayback();
        }
    }
    
    /**
     * 释放所有资源
     */
    public void release() {
        for (VideoPreviewView previewView : previewViews) {
            previewView.release();
        }
        previewViews.clear();
        currentDevices.clear();
    }
    
    /**
     * 获取当前显示的设备数量
     */
    public int getVisibleDeviceCount() {
        return Math.min(previewViews.size(), currentDevices.size());
    }
    
    /**
     * 获取最大可显示的设备数量
     */
    public int getMaxDeviceCount() {
        return previewViews.size();
    }
    
    /**
     * 检查是否可以添加更多设备
     */
    public boolean canAddMoreDevices() {
        return currentDevices.size() < previewViews.size();
    }
}
