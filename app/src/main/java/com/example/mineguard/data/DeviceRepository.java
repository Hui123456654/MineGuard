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
        deviceList.add(new DeviceItem("异物相机1", "东区","192.168.31.64", "异物", "异物相机",  "rtsp://192.168.31.64/live/raw"));
        deviceList.add(new DeviceItem("煤量相机2", "北区", "192.168.1.65", "煤块", "煤量相机", "rtsp://192.168.1.65/live/raw"));
        deviceList.add(new DeviceItem("三超相机3", "西区", "192.168.1.103", "三超", "三超相机", "rtsp://192.168.1.103/live/raw"));
    }

    // --- CRUD 操作方法 (操作后必须调用 saveDeviceData) ---

    public List<DeviceItem> getDevices() {
        return Collections.unmodifiableList(deviceList);
    }

    /** * 检查名称是否重复
     * @param name 要检查的名字
     * @param excludeItem 如果是修改操作，需要排除掉当前正在修改的对象本身
     */
    private boolean isNameExists(String name, DeviceItem excludeItem) {
        for (DeviceItem item : deviceList) {
            if (item.getDeviceName().equals(name)) {
                // 如果是新增，excludeItem 为 null；如果是修改，判断是否为同一个对象
                if (excludeItem == null || !excludeItem.getDeviceName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addDevice(DeviceItem item) {
        // 检查名称是否已存在
        if (isNameExists(item.getDeviceName(), null)) {
            return false; // 添加失败
        }
        deviceList.add(item);
        saveDeviceData();
        return true;
    }

    public boolean updateDevice(DeviceItem oldItem, DeviceItem newItem) {
        // 1. 查找旧对象索引
        int index = deviceList.indexOf(oldItem);
        if (index == -1) return false;

        // 2. 检查新名称是否与其他设备冲突 (排除掉自己)
        // 注意：如果用户没改名字只是改了IP，isNameExists 应该返回 false
        for (DeviceItem item : deviceList) {
            if (item != deviceList.get(index) && item.getDeviceName().equals(newItem.getDeviceName())) {
                return false; // 名称冲突
            }
        }

        deviceList.set(index, newItem);
        saveDeviceData();
        return true;
    }

    public boolean deleteDevice(DeviceItem item) {
        boolean removed = deviceList.remove(item);
        if (removed) {
            saveDeviceData(); // <-- 每次删除后，立即保存到磁盘
        }
        return removed;
    }
}