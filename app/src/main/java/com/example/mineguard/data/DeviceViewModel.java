// DeviceViewModel.java (新增文件)
package com.example.mineguard.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class DeviceViewModel extends ViewModel {

    private final DeviceRepository repository = DeviceRepository.getInstance();

    // 使用 MutableLiveData 来持有和发布数据变化
    private final MutableLiveData<List<DeviceItem>> liveDeviceList = new MutableLiveData<>();

    public DeviceViewModel() {
        // 初始化时从 Repository 加载数据
        liveDeviceList.setValue(repository.getDevices());
    }

    /** 对外暴露不可修改的 LiveData */
    public LiveData<List<DeviceItem>> getLiveDeviceList() {
        return liveDeviceList;
    }

    /** 刷新 LiveData */
    private void refreshLiveData() {
        // 任何对 repository 的修改完成后，都调用此方法通知 LiveData
        liveDeviceList.setValue(repository.getDevices());
    }

    // --- 供 Fragment 调用的 CRUD 方法 ---

    public boolean addDevice(DeviceItem item) {
        boolean success = repository.addDevice(item);
        if (success) {
            refreshLiveData();
        }
        return success; // 返回结果给 Fragment
    }

    public boolean updateDevice(DeviceItem oldItem, DeviceItem newItem) {
        boolean success = repository.updateDevice(oldItem, newItem);
        if (success) {
            refreshLiveData();
        }
        return success; // 返回结果给 Fragment
    }

    public void deleteDevice(DeviceItem item) {
        if (repository.deleteDevice(item)) {
            refreshLiveData();
        }
    }
}