package com.example.mineguard.alarm.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.mineguard.R;
import com.example.mineguard.data.DeviceItem;
import com.example.mineguard.data.DeviceRepository;
import java.util.ArrayList;
import java.util.List;
/**
 * 筛选条件对话框
 * 修改记录：改为居中弹窗 (DialogFragment)，并适配宽屏显示
 */
public class FilterDialog extends DialogFragment {

    private Spinner spinnerAlarmType;
    private Spinner spinnerAlarmLevel;
    private Spinner spinnerStatus;
    private Spinner spinnerLocation;
    private Button btnReset;
    private Button btnConfirm;

    private String selectedAlarmType;
    private String selectedAlarmLevel;
    private String selectedStatus;
    private String selectedLocation;

    private OnFilterChangeListener listener;

    public interface OnFilterChangeListener {
        void onFilterChanged(String alarmType, String alarmLevel, String status, String location);
    }

    public static FilterDialog newInstance(String alarmType, String alarmLevel, String status, String location) {
        FilterDialog dialog = new FilterDialog();
        Bundle args = new Bundle();
        args.putString("alarmType", alarmType);
        args.putString("alarmLevel", alarmLevel);
        args.putString("status", status);
        args.putString("location", location);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 设置样式：无标题，且背景透明（由 XML 中的 CardView 处理背景）
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);

        Bundle args = getArguments();
        if (args != null) {
            selectedAlarmType = args.getString("alarmType", "");
            selectedAlarmLevel = args.getString("alarmLevel", "");
            selectedStatus = args.getString("status", "");
            selectedLocation = args.getString("location", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_filter, container, false);

        initViews(view);
        setupSpinners();
        setupClickListeners();
        setSelectedValues();

        return view;
    }

    /**
     * 2. 核心方法：设置弹窗大小和位置
     */
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();

            // 获取屏幕宽度
            DisplayMetrics dm = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

            // 设置宽度为屏幕宽度的 50% (在 1200px 屏幕上约为 600px，非常合适)
            int width = (int) (dm.widthPixels * 0.5);

            // 应用尺寸：宽度固定，高度自适应内容
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

            // 设置背景透明，确保 XML 里的 CardView 圆角能显示出来
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void initViews(View view) {
        spinnerAlarmType = view.findViewById(R.id.spinnerAlarmType);
        spinnerAlarmLevel = view.findViewById(R.id.spinnerAlarmLevel);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        spinnerLocation = view.findViewById(R.id.spinnerLocation);
        btnReset = view.findViewById(R.id.btnReset);
        btnConfirm = view.findViewById(R.id.btnConfirm);
    }

    private void setupSpinners() {
        // 报警类型
        String[] alarmTypes = {"全部", "异物", "煤块", "三超"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, alarmTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarmType.setAdapter(typeAdapter);

        // 报警等级
        String[] alarmLevels = {"全部", "警告", "严重"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, alarmLevels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarmLevel.setAdapter(levelAdapter);

        // 处理状态
        String[] statuses = {"全部", "未处理", "已处理","误报"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // 报警位置
        // 1. 初始化动态列表
        List<String> dynamicAreas = new ArrayList<>();
        dynamicAreas.add("全部");
        // 2. 获取数据
        DeviceRepository repository = DeviceRepository.getInstance();
        if (repository != null) {
            List<DeviceItem> devices = repository.getDevices();
            if (devices != null) {
                for (DeviceItem device : devices) {
                    String area = device.getArea();
                    // 去重且防空处理
                    if (area != null && !area.trim().isEmpty() && !dynamicAreas.contains(area)) {
                        dynamicAreas.add(area);
                    }
                }
            }
        }
        // 2. 将 List 转换为 String[]
        String[] locations = dynamicAreas.toArray(new String[0]);
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);
    }

    private void setupClickListeners() {
        btnReset.setOnClickListener(v -> {
            spinnerAlarmType.setSelection(0);
            spinnerAlarmLevel.setSelection(0);
            spinnerStatus.setSelection(0);
            spinnerLocation.setSelection(0);
        });

        btnConfirm.setOnClickListener(v -> {
            String alarmType = spinnerAlarmType.getSelectedItem().toString();
            String alarmLevel = spinnerAlarmLevel.getSelectedItem().toString();
            String status = spinnerStatus.getSelectedItem().toString();
            String location = spinnerLocation.getSelectedItem().toString();

            // 转换"全部"为空字符串
            alarmType = "全部".equals(alarmType) ? "" : alarmType;
            alarmLevel = "全部".equals(alarmLevel) ? "" : alarmLevel;
            status = "全部".equals(status) ? "" : status;
            location = "全部".equals(location) ? "" : location;

            if (listener != null) {
                listener.onFilterChanged(alarmType, alarmLevel, status, location);
            }
            dismiss();
        });
    }

    private void setSelectedValues() {
        // 设置之前选择的值
        if (!selectedAlarmType.isEmpty()) {
            setSpinnerSelection(spinnerAlarmType, selectedAlarmType);
        }
        if (!selectedAlarmLevel.isEmpty()) {
            setSpinnerSelection(spinnerAlarmLevel, selectedAlarmLevel);
        }
        if (!selectedStatus.isEmpty()) {
            setSpinnerSelection(spinnerStatus, selectedStatus);
        }
        if (!selectedLocation.isEmpty()) {
            setSpinnerSelection(spinnerLocation, selectedLocation);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    public void setOnFilterChangeListener(OnFilterChangeListener listener) {
        this.listener = listener;
    }
}