package com.example.mineguard.analysis;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.PopupMenu;
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
        DeviceItem deviceItem = deviceList.get(position); // 获取 DeviceItem
        // 使用 DeviceItem 的 getter 方法设置文本
        holder.tvName.setText(deviceItem.getDeviceName());
        holder.tvIp.setText(deviceItem.getIpAddress()); // 使用 IP 地址

        // --- 核心修复逻辑：显式处理显示与隐藏 ---
        if ("异物相机".equals(deviceItem.getDeviceType())) {
            // 情况 A：确实是异物相机，执行原有显示和点击逻辑
            holder.tvStatus.setVisibility(View.VISIBLE);

            // 1. 设置文本和样式 (保留原有逻辑)
            String statusText = "已布防".equals(deviceItem.getStatus()) ? "● 已布防" : "○ 已撤防";
            holder.tvStatus.setText(statusText);
            int color = "已布防".equals(deviceItem.getStatus()) ?
                    Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(color));

            // 2. 状态切换点击事件 (保留原有 HTTP 请求触发逻辑)
            holder.tvStatus.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                if ("已布防".equals(deviceItem.getStatus())) {
                    popup.getMenu().add("撤防");
                } else {
                    popup.getMenu().add("布防");
                }

                popup.setOnMenuItemClickListener(item -> {
                    String target = item.getTitle().toString().contains("布防") ? "已布防" : "已撤防";
                    if (listener != null) {
                        // 这里会触发 AnalysisFragment 中的回调，进而调用 ViewModel 发送请求
                        listener.onStatusToggle(deviceItem, target);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            // 情况 B：煤量、三超相机等。
            // 关键修复：必须强制设为 GONE，并清除点击监听，防止复用污染和误触
            holder.tvStatus.setVisibility(View.GONE);
            holder.tvStatus.setOnClickListener(null);
        }


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
        TextView tvName, tvIp,tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvIp = itemView.findViewById(R.id.tv_device_ip);
            tvStatus = itemView.findViewById(R.id.tv_defense_status); // 绑定 ID
        }
    }

    // 1. 定义点击回调接口
    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
        void onStatusToggle(DeviceItem device, String newStatus); // 新增状态切换回调
    }

    // 2. 提供设置监听器的方法
    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }


}