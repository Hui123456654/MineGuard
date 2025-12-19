package com.example.mineguard.configuration;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;
import com.example.mineguard.data.DeviceItem;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<DeviceItem> deviceList;
    private OnItemClickListener listener;

    // 点击事件接口
    public interface OnItemClickListener {
        void onItemClick(DeviceItem item);
    }

    public DeviceAdapter(List<DeviceItem> list, OnItemClickListener listener) {
        this.deviceList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_list, parent, false);
        return new DeviceViewHolder(view);
    }

    //    @Override
//    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
//        DeviceItem item = deviceList.get(position);
//
//        holder.tvName.setText(item.getDeviceName());
//        holder.tvArea.setText("所属区域: " + item.getArea());
//        holder.tvIp.setText("IP地址: " + item.getIpAddress());
//        holder.tvDevice.setText("设备类型: " + item.getDeviceType());
//        holder.tvAlarm.setText("报警类型: " + item.getAlarmType());
//
//        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
//    }
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceItem item = deviceList.get(position);

        // 设置加粗的大标题
        holder.tvName.setText("设备名称: " + item.getDeviceName());

        // 使用 Emoji 或图标增加识别度，不再携带长前缀
        holder.tvArea.setText("📍 所属区域： " + item.getArea());
        holder.tvIp.setText("🌐 IP地址： " + item.getIpAddress());
        holder.tvDevice.setText("📱 设备类型： " + item.getDeviceType());
        holder.tvAlarm.setText("⚠️ 报警类型： " + item.getAlarmType());

        // 点击事件保持不变
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvArea, tvIp, tvDevice, tvAlarm;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.item_device_name);
            tvArea = itemView.findViewById(R.id.item_area);
            tvIp = itemView.findViewById(R.id.item_ip);
            tvDevice = itemView.findViewById(R.id.item_device_type);
            tvAlarm = itemView.findViewById(R.id.item_alarm);
        }
    }
}