package com.example.mineguard.data;

public class DeviceItem {
    // 基础信息
    private String deviceName;
    private String area;
    private String ipAddress;
    private String alarmType;
    private String deviceType;
    private String rtspUrl;

    public DeviceItem(String deviceName, String area, String ipAddress, String alarmType, String deviceType,  String rtspUrl) {
        this.deviceName = deviceName;
        this.area = area;
        this.ipAddress = ipAddress;
        this.alarmType = alarmType;
        this.deviceType = deviceType;
        this.rtspUrl = rtspUrl;
    }

    // Getters and Setters
    public String getDeviceName() { return deviceName; }
    public String getArea() { return area; }
    public String getIpAddress() { return ipAddress; }
    public String getAlarmType() { return alarmType; }
    public String getDeviceType() { return deviceType; }
    public String getRtspUrl() { return rtspUrl; }
    // 新增：重写 equals() 和 hashCode()，用于 List.remove() 和 List.update()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceItem that = (DeviceItem) o;
        // 假设设备名称是唯一的标识符，用于识别同一个设备
        return deviceName.equals(that.deviceName);
    }

    @Override
    public int hashCode() {
        return deviceName.hashCode();
    }
}