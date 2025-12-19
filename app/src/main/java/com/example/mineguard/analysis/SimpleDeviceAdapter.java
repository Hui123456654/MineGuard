package com.example.mineguard.analysis;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;
import com.example.mineguard.data.DeviceItem;
import java.util.List;

/**
 * 左侧设备列表适配器 (视觉优化版)
 */
public class SimpleDeviceAdapter extends RecyclerView.Adapter<SimpleDeviceAdapter.ViewHolder> {

    private List<DeviceItem> deviceList;
    private OnDeviceClickListener listener;
    private int selectedPosition = 0;
    public SimpleDeviceAdapter(List<DeviceItem> deviceList) {
        this.deviceList = deviceList;
    }
    // 新增 setDeviceList 方法，用于 LiveData 更新
    public void setDeviceList(List<DeviceItem> newDeviceList) {
        this.deviceList = newDeviceList;
        // 通知 RecyclerView 整个数据集已更改
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //String deviceName = deviceList.get(position);
        DeviceItem deviceItem = deviceList.get(position); // 获取 DeviceItem
        // 使用 DeviceItem 的 getter 方法设置文本
        holder.tvName.setText(deviceItem.getDeviceName());
        holder.tvIp.setText(deviceItem.getIpAddress()); // 使用 IP 地址

        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.parseColor("#33AAAAAA")); // 浅灰i色背景
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) listener.onDeviceClick(deviceItem);
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvIp = itemView.findViewById(R.id.tv_device_ip);
        }
    }

    // 1. 定义点击回调接口
    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    // 2. 提供设置监听器的方法
    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

}