package com.example.mineguard.data;

public class DeviceItem {
    // 基础信息
    private String deviceName;
    private String area;
    private String status;
    private String ipAddress;
    private String alarmType;
    private String deviceType;
    private String algoServer;
    // 详细信息（点击后显示）
    private String port;
    private String username;
    private String password;
    private String rtspUrl;

    public DeviceItem(String deviceName, String area, String status,String ipAddress, String alarmType, String deviceType, String algoServer, String port, String username, String password, String rtspUrl) {
        this.deviceName = deviceName;
        this.area = area;
        this.status = status;
        this.ipAddress = ipAddress;
        this.alarmType = alarmType;
        this.deviceType = deviceType;
        this.algoServer = algoServer;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rtspUrl = rtspUrl;
        this.status = status;
    }

    // Getters and Setters (可以使用IDE自动生成)
    public String getDeviceName() { return deviceName; }
    public String getArea() { return area; }
    public String getIpAddress() { return ipAddress; }
    public String getAlarmType() { return alarmType; }
    public String getDeviceType() { return deviceType; }
    public String getAlgoServer() { return algoServer; }
    public String getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRtspUrl() { return rtspUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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