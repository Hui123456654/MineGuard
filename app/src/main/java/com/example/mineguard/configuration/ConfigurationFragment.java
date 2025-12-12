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

    // 表单控件
    private Spinner spDevice;
    private EditText etName, etArea,etStatus, etAlgo, etIp, etPort, etUser, etPass, etAlarm, etRtsp;
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

        // 绑定表单
        etName = view.findViewById(R.id.et_device_name);
        etArea = view.findViewById(R.id.et_area);
        etStatus = view.findViewById(R.id.et_status);
        etAlgo = view.findViewById(R.id.et_algo_server);
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
        etPort = view.findViewById(R.id.et_port);
        etUser = view.findViewById(R.id.et_username);
        etPass = view.findViewById(R.id.et_password);
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

        // 2. 填充数据
        etName.setText(item.getDeviceName());
        etArea.setText(item.getArea());
        etStatus.setText(item.getStatus());
        etAlgo.setText(item.getAlgoServer());
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
        etPort.setText(item.getPort());
        etUser.setText(item.getUsername());
        etPass.setText(item.getPassword());
        etAlarm.setText(item.getAlarmType());
        etRtsp.setText(item.getRtspUrl());
    }
    private DeviceItem getDeviceItemFromForm() {
        // 从表单获取数据并创建一个新的 DeviceItem 对象
        String name = etName.getText().toString();
        String area = etArea.getText().toString();
        String status = etStatus.getText().toString();
        String algo = etAlgo.getText().toString();
        String deviceType = spDevice.getSelectedItem().toString();
        String ip = etIp.getText().toString();
        String port = etPort.getText().toString();
        String user = etUser.getText().toString();
        String pass = etPass.getText().toString();
        String alarm = etAlarm.getText().toString();
        String rtsp = etRtsp.getText().toString();

        return new DeviceItem(name, area, status,ip, alarm, deviceType, algo, port, user, pass, rtsp);
    }

    private void setupButtons() {
        // 增加按钮：
        btnAdd.setOnClickListener(v -> {
            // 假设用户在清空表单后点击增加，则将当前表单内容作为新设备添加
            DeviceItem newItem = getDeviceItemFromForm();
            if (newItem.getDeviceName().isEmpty()) {
                Toast.makeText(getContext(), "设备名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.addDevice(newItem); // <-- 调用 ViewModel 执行添加
            Toast.makeText(getContext(), "已新增通道：" + newItem.getDeviceName(), Toast.LENGTH_SHORT).show();
            clearForm();
        });

        // 修改按钮：
        btnModify.setOnClickListener(v -> {
            if (currentSelectedItem == null) {
                Toast.makeText(getContext(), "请先选择要修改的通道", Toast.LENGTH_SHORT).show();
                return;
            }
            // 使用当前选中的设备作为旧项，表单内容作为新项
            DeviceItem newItem = getDeviceItemFromForm();
            viewModel.updateDevice(currentSelectedItem, newItem); // <-- 调用 ViewModel 执行修改
            currentSelectedItem = newItem; // 更新当前选中项的引用
            Toast.makeText(getContext(), "已更新通道信息：" + newItem.getDeviceName(), Toast.LENGTH_SHORT).show();
        });

        // 删除按钮：
        btnDelete.setOnClickListener(v -> {
            if (currentSelectedItem == null) {
                Toast.makeText(getContext(), "请先选择要删除的通道", Toast.LENGTH_SHORT).show();
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
        etStatus.setText("");
        etAlgo.setText("");
        spDevice.setSelection(0);
        etIp.setText("");
        etPort.setText("");
        etUser.setText("");
        etPass.setText("");
        etAlarm.setText("");
        etRtsp.setText("");
    }
}