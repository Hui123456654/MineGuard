package com.example.mineguard.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeviceRepository {

    // SharedPreferences 文件名和 Key
    private static final String PREFS_NAME = "mine_app_config";
    private static final String KEY_DEVICE_LIST = "device_list_data";

    // 核心数据：内存缓存
    private static List<DeviceItem> deviceList = new ArrayList<>();

    // 单例模式
    private static DeviceRepository INSTANCE;
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    // 构造函数现在需要 Context
    private DeviceRepository(Context context) {
        // 使用 Application Context 避免内存泄漏
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadDeviceData(); // 应用启动时，从磁盘加载数据
    }

    /** 必须在应用启动时（如 MainActivity 的 onCreate）调用一次 init 方法 */
    public static void init(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DeviceRepository(context);
        }
    }

    public static DeviceRepository getInstance() {
        if (INSTANCE == null) {
            // 如果忘记在 MainActivity 中初始化，则抛出异常
            throw new IllegalStateException("DeviceRepository 尚未初始化，请先在应用启动时调用 init(Context)。");
        }
        return INSTANCE;
    }

    // --- 持久化方法 ---

    /** 从 SharedPreferences 加载数据 */
    private void loadDeviceData() {
        String json = preferences.getString(KEY_DEVICE_LIST, null);

        if (json != null) {
            // 1. 从 JSON 字符串反序列化为 List<DeviceItem>
            Type listType = new TypeToken<List<DeviceItem>>() {}.getType();
            deviceList = gson.fromJson(json, listType);
        }

        // 2. 如果 SharedPreferences 中没有数据 (首次运行)，则加载硬编码的默认数据
        if (deviceList == null || deviceList.isEmpty()) {
            initDefaultData();
            saveDeviceData(); // 立即保存默认数据到磁盘
        }
    }

    /** 保存数据到 SharedPreferences */
    private void saveDeviceData() {
        // 1. 将 List<DeviceItem> 序列化为 JSON 字符串
        String json = gson.toJson(deviceList);
        // 2. 写入 SharedPreferences
        preferences.edit().putString(KEY_DEVICE_LIST, json).apply();
    }


    // --- 新增：强制重置数据方法 ---
    /**
     * 清除 SharedPreferences 中的持久化数据，并重新加载默认数据。
     * 警告：该方法会清除所有用户配置的设备信息。
     */
    public void resetData() {
        // 1. 清除 SharedPreferences 中的数据
        preferences.edit().remove(KEY_DEVICE_LIST).apply();

        // 2. 重新初始化默认数据到内存
        initDefaultData();

        // 3. 将新的默认数据立即保存到 SharedPreferences
        saveDeviceData();
    }

    /** 初始化默认数据（从 ConfigurationFragment 迁移过来的初始数据） */
    private void initDefaultData() {
        // 确保列表是新的
        deviceList = new ArrayList<>();
        deviceList.add(new DeviceItem("异物相机1", "东区", "在线","192.168.31.64", "异物", "异物相机", "Algo_Svr_A", "5002", "admin", "kr83890168", "rtsp://192.168.31.64/live/raw"));
        deviceList.add(new DeviceItem("煤量相机2", "北区", "离线","192.168.1.65", "煤块", "煤量相机", "Algo_Svr_B", "5002", "guest", "abcde", "rtsp://192.168.1.65/live/raw"));
        deviceList.add(new DeviceItem("煤三超相机3", "西区", "离线","192.168.1.103", "三超", "三超相机", "Algo_Svr_A", "5002", "admin", "12345", "rtsp://192.168.1.103/live/raw"));
    }

    // --- CRUD 操作方法 (操作后必须调用 saveDeviceData) ---

    public List<DeviceItem> getDevices() {
        return Collections.unmodifiableList(deviceList);
    }

    public void addDevice(DeviceItem item) {
        deviceList.add(item);
        saveDeviceData(); // <-- 每次添加后，立即保存到磁盘
    }

    public boolean updateDevice(DeviceItem oldItem, DeviceItem newItem) {
        // 使用 DeviceItem 中重写的 equals() 方法来查找对象
        int index = deviceList.indexOf(oldItem);

        if (index != -1) {
            deviceList.set(index, newItem);
            saveDeviceData(); // <-- 每次修改后，立即保存到磁盘
            return true;
        }
        return false;
    }

    public boolean deleteDevice(DeviceItem item) {
        boolean removed = deviceList.remove(item);
        if (removed) {
            saveDeviceData(); // <-- 每次删除后，立即保存到磁盘
        }
        return removed;
    }
}