package com.example.mineguard.configuration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- 新增导入
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;
import com.example.mineguard.data.DeviceItem;
import com.example.mineguard.data.DeviceViewModel;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationFragment extends Fragment {

    private RecyclerView rvDeviceList;
    private DeviceAdapter adapter;
    private List<DeviceItem> dataList = new ArrayList<>(); // 初始化为空列表，等待 LiveData 赋值
    private DeviceViewModel viewModel; // <-- 新增 ViewModel 成员
    private DeviceItem currentSelectedItem = null; // <-- 新增：用于跟踪当前选中的设备

    // 右侧视图组件
    private TextView tvEmptyHint;
    private View layoutDetailForm;
    private Button btnNavToAdd;
    // 表单控件
    private Spinner spDevice;
    private EditText etName, etArea,etIp,etAlarm, etRtsp;
    private final String[] deviceOptions = {"煤量相机", "异物相机", "三超相机"};
    private Button btnAdd, btnModify, btnDelete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化 ViewModel (使用 requireActivity() 范围)
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        initViews(view);
        setupRecyclerView();
        // 2. 观察 ViewModel 的 LiveData
        observeViewModel();
        setupButtons();
    }

    private void observeViewModel() {
        // 观察 LiveData，数据变化时自动更新 UI
        viewModel.getLiveDeviceList().observe(getViewLifecycleOwner(), deviceItems -> {
            dataList.clear();
            dataList.addAll(deviceItems);
            adapter.notifyDataSetChanged();

            // 如果数据列表为空，显示提示
            if (dataList.isEmpty()) {
                tvEmptyHint.setVisibility(View.VISIBLE);
                layoutDetailForm.setVisibility(View.GONE);
            }
        });
    }

    private void initViews(View view) {
        rvDeviceList = view.findViewById(R.id.rv_device_list);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        layoutDetailForm = view.findViewById(R.id.layout_detail_form);
        btnNavToAdd = view.findViewById(R.id.btn_nav_to_add);
        // 绑定表单
        etName = view.findViewById(R.id.et_device_name);
        etArea = view.findViewById(R.id.et_area);
        spDevice = view.findViewById(R.id.sp_device_type);
        // 创建适配器
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                deviceOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDevice.setAdapter(adapter);
        etIp = view.findViewById(R.id.et_ip_address);
        etAlarm = view.findViewById(R.id.et_alarm_type);
        etRtsp = view.findViewById(R.id.et_rtsp);
        btnAdd = view.findViewById(R.id.btn_add);
        btnModify = view.findViewById(R.id.btn_modify);
        btnDelete = view.findViewById(R.id.btn_delete);
    }

    private void setupRecyclerView() {
        rvDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceAdapter(dataList, item -> {
            // 点击列表项回调
            currentSelectedItem = item; // <-- 保存当前选中项
            showDeviceDetails(item);
        });
        rvDeviceList.setAdapter(adapter);
    }

    private void showDeviceDetails(DeviceItem item) {
        // 1. 隐藏“请选择”，显示表单
        tvEmptyHint.setVisibility(View.GONE);
        layoutDetailForm.setVisibility(View.VISIBLE);

        // 切换按钮：隐藏增加，显示修改/删除
        btnAdd.setVisibility(View.GONE);
        btnModify.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);

        // 2. 填充数据
        etName.setText(item.getDeviceName());
        etArea.setText(item.getArea());
        // 4. 设置 Spinner 选中项
        String currentDevice = item.getDeviceType();
        if (currentDevice != null) {
            for (int i = 0; i < deviceOptions.length; i++) {
                if (deviceOptions[i].equals(currentDevice)) {
                    spDevice.setSelection(i);
                    break;
                }
            }
        }
        etIp.setText(item.getIpAddress());
        etAlarm.setText(item.getAlarmType());
        etRtsp.setText(item.getRtspUrl());
    }
    private DeviceItem getDeviceItemFromForm() {
        // 从表单获取数据并创建一个新的 DeviceItem 对象
        String name = etName.getText().toString();
        String area = etArea.getText().toString();
        String deviceType = spDevice.getSelectedItem().toString();
        String ip = etIp.getText().toString();
        String alarm = etAlarm.getText().toString();
        String rtsp = etRtsp.getText().toString();
        return new DeviceItem(name, area,ip, alarm, deviceType, rtsp, "撤防");
    }

    private void setupButtons() {
        // 标题旁边的“+ 添加设备”按钮逻辑
        btnNavToAdd.setOnClickListener(v -> {
            currentSelectedItem = null; // 确保清除选中状态
            clearForm();
            tvEmptyHint.setVisibility(View.GONE);
            layoutDetailForm.setVisibility(View.VISIBLE);

            // 设置按钮显示逻辑：显示增加，隐藏修改/删除
            btnAdd.setVisibility(View.VISIBLE);
            btnModify.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        });

        // 增加按钮点击逻辑
        btnAdd.setOnClickListener(v -> {
            DeviceItem newItem = getDeviceItemFromForm();
            if (newItem.getDeviceName().isEmpty()) {
                Toast.makeText(getContext(), "设备名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 接收 ViewModel 的返回值
            boolean success = viewModel.addDevice(newItem);

            if (success) {
                if (getActivity() instanceof com.example.mineguard.MainActivity) {
                    ((com.example.mineguard.MainActivity) getActivity()).manualRefreshAlarmConfig();
                }
                Toast.makeText(getContext(), "设备已添加：" + newItem.getDeviceName(), Toast.LENGTH_SHORT).show();
                layoutDetailForm.setVisibility(View.GONE);
                tvEmptyHint.setVisibility(View.VISIBLE);
            } else {
                // 提示冲突
                Toast.makeText(getContext(), "添加失败：设备名称「" + newItem.getDeviceName() + "」已存在", Toast.LENGTH_LONG).show();
            }
        });

        // 修改按钮点击逻辑
        btnModify.setOnClickListener(v -> {
            if (currentSelectedItem == null) return;

            DeviceItem newItem = getDeviceItemFromForm();
            // 核心修复：将旧对象的状态赋值给新对象，确保状态不被覆盖
            newItem.setStatus(currentSelectedItem.getStatus());
            // 接收 ViewModel 的返回值
            boolean success = viewModel.updateDevice(currentSelectedItem, newItem);

            if (success) {
                currentSelectedItem = newItem;
                if (getActivity() instanceof com.example.mineguard.MainActivity) {
                    ((com.example.mineguard.MainActivity) getActivity()).manualRefreshAlarmConfig();
                }
                Toast.makeText(getContext(), "已更新设备信息", Toast.LENGTH_SHORT).show();
            } else {
                // 提示冲突
                Toast.makeText(getContext(), "修改失败：名称「" + newItem.getDeviceName() + "」与其他设备冲突", Toast.LENGTH_LONG).show();
            }
        });

        // 删除按钮：
        btnDelete.setOnClickListener(v -> {
            if (currentSelectedItem == null) {
                Toast.makeText(getContext(), "请先选择要删除的设备", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.deleteDevice(currentSelectedItem); // <-- 调用 ViewModel 执行删除
            Toast.makeText(getContext(), "已删除选中条目：" + currentSelectedItem.getDeviceName(), Toast.LENGTH_SHORT).show();
            currentSelectedItem = null; // 清空选中状态
            tvEmptyHint.setVisibility(View.VISIBLE);
            layoutDetailForm.setVisibility(View.GONE);
        });
    }

    private void clearForm() {
        etName.setText("");
        etArea.setText("");
        spDevice.setSelection(0);
        etIp.setText("");
        etAlarm.setText("");
        etRtsp.setText("");
    }
}