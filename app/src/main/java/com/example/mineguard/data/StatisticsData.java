package com.example.mineguard.data;

import java.util.List;

public class StatisticsData {
    private int totalAlarms;
    private int unprocessedAlarms;
    private int totalDevices;
    private int onlineDevices;
    private List<AlarmTrendData> alarmTrendData;
    private List<SceneAlarmData> sceneAlarmData;
    private List<DeviceAlarmData> deviceAlarmData;
    private List<AlarmTypeData> alarmTypeData;

    public StatisticsData() {}

    public StatisticsData(int totalAlarms, int unprocessedAlarms, int totalDevices, int onlineDevices) {
        this.totalAlarms = totalAlarms;
        this.unprocessedAlarms = unprocessedAlarms;
        this.totalDevices = totalDevices;
        this.onlineDevices = onlineDevices;
    }

    // Getters and Setters
    public int getTotalAlarms() {
        return totalAlarms;
    }

    public void setTotalAlarms(int totalAlarms) {
        this.totalAlarms = totalAlarms;
    }

    public int getUnProcessedAlarms() {
        return unprocessedAlarms;
    }

    public void setUnProcessedAlarms(int unprocessedAlarms) {
        this.unprocessedAlarms = unprocessedAlarms;
    }

    public int getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(int totalDevices) {
        this.totalDevices = totalDevices;
    }

    public int getOnlineDevices() {
        return onlineDevices;
    }

    public void setOnlineDevices(int onlineDevices) {
        this.onlineDevices = onlineDevices;
    }

    public List<AlarmTrendData> getAlarmTrendData() {
        return alarmTrendData;
    }

    public void setAlarmTrendData(List<AlarmTrendData> alarmTrendData) {
        this.alarmTrendData = alarmTrendData;
    }

    public List<SceneAlarmData> getSceneAlarmData() {
        return sceneAlarmData;
    }

    public void setSceneAlarmData(List<SceneAlarmData> sceneAlarmData) {
        this.sceneAlarmData = sceneAlarmData;
    }

    public List<DeviceAlarmData> getDeviceAlarmData() {
        return deviceAlarmData;
    }

    public void setDeviceAlarmData(List<DeviceAlarmData> deviceAlarmData) {
        this.deviceAlarmData = deviceAlarmData;
    }

    public List<AlarmTypeData> getAlarmTypeData() {
        return alarmTypeData;
    }

    public void setAlarmTypeData(List<AlarmTypeData> alarmTypeData) {
        this.alarmTypeData = alarmTypeData;
    }

    // Inner classes for different data types
    public static class AlarmTrendData {
        private String timeLabel;
        private int alarmCount;

        public AlarmTrendData(String timeLabel, int alarmCount) {
            this.timeLabel = timeLabel;
            this.alarmCount = alarmCount;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public void setTimeLabel(String timeLabel) {
            this.timeLabel = timeLabel;
        }

        public int getAlarmCount() {
            return alarmCount;
        }

        public void setAlarmCount(int alarmCount) {
            this.alarmCount = alarmCount;
        }
    }

    public static class SceneAlarmData {
        private String sceneName;
        private int alarmCount;

        public SceneAlarmData(String sceneName, int alarmCount) {
            this.sceneName = sceneName;
            this.alarmCount = alarmCount;
        }

        public String getSceneName() {
            return sceneName;
        }

        public void setSceneName(String sceneName) {
            this.sceneName = sceneName;
        }

        public int getAlarmCount() {
            return alarmCount;
        }

        public void setAlarmCount(int alarmCount) {
            this.alarmCount = alarmCount;
        }
    }

    public static class DeviceAlarmData {
        private String deviceName;
        private int alarmCount;

        public DeviceAlarmData(String deviceName, int alarmCount) {
            this.deviceName = deviceName;
            this.alarmCount = alarmCount;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public int getAlarmCount() {
            return alarmCount;
        }

        public void setAlarmCount(int alarmCount) {
            this.alarmCount = alarmCount;
        }
    }

    public static class AlarmTypeData {
        private String typeName;
        private int alarmCount;

        public AlarmTypeData(String typeName, int alarmCount) {
            this.typeName = typeName;
            this.alarmCount = alarmCount;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public int getAlarmCount() {
            return alarmCount;
        }

        public void setAlarmCount(int alarmCount) {
            this.alarmCount = alarmCount;
        }
    }
}
