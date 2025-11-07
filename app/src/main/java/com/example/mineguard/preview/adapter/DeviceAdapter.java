package com.example.mineguard.preview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mineguard.R;
import com.example.mineguard.preview.model.DeviceItem;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 设备列表适配器，按区域分组显示设备，支持折叠面板
 */
public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DEVICE = 1;
    
    private List<DeviceItem> deviceList;
    private List<String> regionList;
    private Map<String, List<DeviceItem>> regionDeviceMap;
    private Map<String, Boolean> regionExpandedMap; // 记录每个区域的展开状态
    private List<Object> itemList; // 包含header和device的混合列表
    private OnDeviceClickListener listener;
    
    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
        void onDeviceLongClick(DeviceItem device);
        void onQuickViewClick(String region, List<DeviceItem> devices);
    }
    
    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
        this.deviceList = new ArrayList<>();
        this.regionList = new ArrayList<>();
        this.regionDeviceMap = new HashMap<>();
        this.regionExpandedMap = new HashMap<>();
        this.itemList = new ArrayList<>();
    }
    
    public void updateDevices(List<DeviceItem> devices) {
        this.deviceList.clear();
        this.deviceList.addAll(devices);
        groupDevicesByRegion();
        notifyDataSetChanged();
    }
    
    private void groupDevicesByRegion() {
        regionList.clear();
        regionDeviceMap.clear();
        regionExpandedMap.clear();
        itemList.clear();
        
        // 按区域分组，只包含摄像头设备
        for (DeviceItem device : deviceList) {
            // 只保留摄像头设备，删除传感器和报警器
            if (device.getType() == DeviceItem.TYPE_CAMERA) {
                String region = device.getRegion();
                if (!regionDeviceMap.containsKey(region)) {
                    regionDeviceMap.put(region, new ArrayList<>());
                    regionList.add(region);
                    regionExpandedMap.put(region, true); // 默认展开
                }
                regionDeviceMap.get(region).add(device);
            }
        }
        
        // 构建显示列表
        for (String region : regionList) {
            itemList.add(region); // 添加区域header
            // 如果该区域展开，则添加设备
            if (regionExpandedMap.get(region)) {
                itemList.addAll(regionDeviceMap.get(region));
            }
        }
    }
    
    private void toggleRegionExpansion(String region) {
        boolean isExpanded = regionExpandedMap.get(region);
        regionExpandedMap.put(region, !isExpanded);
        
        // 重新构建itemList
        itemList.clear();
        for (String r : regionList) {
            itemList.add(r);
            if (regionExpandedMap.get(r)) {
                itemList.addAll(regionDeviceMap.get(r));
            }
        }
        
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        Object item = itemList.get(position);
        return item instanceof String ? TYPE_HEADER : TYPE_DEVICE;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collapsible_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device_card, parent, false);
            return new DeviceViewHolder(view);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
    
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvRegionName;
        TextView tvDeviceCount;
        ImageView ivExpandIndicator;
        Button btnQuickView;
        
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRegionName = itemView.findViewById(R.id.tv_region_name);
            tvDeviceCount = itemView.findViewById(R.id.tv_device_count);
            ivExpandIndicator = itemView.findViewById(R.id.iv_expand_indicator);
            btnQuickView = itemView.findViewById(R.id.btn_quick_view);
        }
        
        public void bind(String region) {
            tvRegionName.setText(region);
            
            // 计算该区域的设备数量
            List<DeviceItem> devices = regionDeviceMap.get(region);
            int onlineCount = 0;
            if (devices != null) {
                for (DeviceItem device : devices) {
                    if (device.isOnline()) {
                        onlineCount++;
                    }
                }
                tvDeviceCount.setText(onlineCount + "/" + devices.size() + " 在线");
            }
            
            // 设置展开/收起图标
            boolean isExpanded = regionExpandedMap.get(region);
            if (isExpanded) {
                ivExpandIndicator.setRotation(0);
            } else {
                ivExpandIndicator.setRotation(-90);
            }
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                toggleRegionExpansion(region);
                // 添加旋转动画
                RotateAnimation rotateAnimation = new RotateAnimation(
                    isExpanded ? 0 : -90,
                    isExpanded ? -90 : 0,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f
                );
                rotateAnimation.setDuration(200);
                ivExpandIndicator.startAnimation(rotateAnimation);
            });
            
            // 设置快速查看按钮点击事件
            btnQuickView.setOnClickListener(v -> {
                if (listener != null && devices != null) {
                    listener.onQuickViewClick(region, devices);
                }
            });
        }
    }
    
    class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeviceIcon;
        TextView tvDeviceName;
        TextView tvDeviceType;
        TextView tvDeviceArea;
        TextView tvDeviceStatus;
        View statusIndicator;
        
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDeviceIcon = itemView.findViewById(R.id.iv_device_icon);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceType = itemView.findViewById(R.id.tv_device_type);
            tvDeviceArea = itemView.findViewById(R.id.tv_device_area);
            tvDeviceStatus = itemView.findViewById(R.id.tv_device_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
        
        public void bind(DeviceItem device) {
            tvDeviceName.setText(device.getName());
            tvDeviceType.setText(device.getTypeName());
            tvDeviceArea.setText(device.getArea());
            tvDeviceStatus.setText(device.getStatusName());
            
            // 只设置摄像头图标
            ivDeviceIcon.setImageResource(R.drawable.ic_camera);
            
            // 设置状态指示器颜色
            switch (device.getStatus()) {
                case DeviceItem.STATUS_ONLINE:
                    statusIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.primary_green));
                    break;
                case DeviceItem.STATUS_OFFLINE:
                    statusIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
                    break;
                case DeviceItem.STATUS_ERROR:
                    statusIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.primary_red));
                    break;
            }
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceLongClick(device);
                }
                return true;
            });
        }
    }
    
    // 更新区域设备数量
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = itemList.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            String region = (String) item;
            headerHolder.bind(region);
        } else if (holder instanceof DeviceViewHolder) {
            DeviceViewHolder deviceHolder = (DeviceViewHolder) holder;
            DeviceItem device = (DeviceItem) item;
            deviceHolder.bind(device);
        }
    }
}
