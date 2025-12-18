package com.example.mineguard.alarm.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.io.Serializable;

// 1. Room 实体注解
@Entity(tableName = "alarm_table")
public class AlarmItem implements Serializable {

    // ================== 2. 补回丢失的常量定义 ==================
    public static final int STATUS_UNPROCESSED = 0;  // 未处理
    public static final int STATUS_PROCESSED = 1;    // 已处理
    public static final int STATUS_FALSE_ALARM = 2;  // 误报
    public static final String LEVEL_WARNING = "0";  // 警告级别
    public static final String LEVEL_CRITICAL = "1"; // 严重级别
    // ========================================================

    @PrimaryKey(autoGenerate = true)
    private int dbId; // 数据库自增ID

    private int id;               // 原始报警ID
    private String channel;
    private String type;
    private String level;
    private String algorithm_code;
    private String path;
    private String video_path;

    @Ignore
    private String[] video_paths; // 忽略数组

    private int status;
    private Integer camera_id;
    private String url;
    private String solve_time;
    private String ip;
    private String name;
    private String location;
    private String flow;
    private String processInfo;
    private int dataSource;
    private String exceedType;
    private String totalWeight;
    private String statsDate;

    public AlarmItem() {
    }

    // ================== Getters and Setters (全部补全) ==================

    public int getDbId() { return dbId; }
    public void setDbId(int dbId) { this.dbId = dbId; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getAlgorithm_code() { return algorithm_code; }
    public void setAlgorithm_code(String algorithm_code) { this.algorithm_code = algorithm_code; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getVideo_path() { return video_path; }
    public void setVideo_path(String video_path) { this.video_path = video_path; }

    public String[] getVideo_paths() { return video_paths; }
    public void setVideo_paths(String[] video_paths) { this.video_paths = video_paths; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Integer getCamera_id() { return camera_id; }
    public void setCamera_id(Integer camera_id) { this.camera_id = camera_id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSolve_time() { return solve_time; }
    public void setSolve_time(String solve_time) { this.solve_time = solve_time; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFlow() { return flow; }
    public void setFlow(String flow) { this.flow = flow; }

    public String getProcessInfo() { return processInfo; }
    public void setProcessInfo(String processInfo) { this.processInfo = processInfo; }

    public int getDataSource() { return dataSource; }
    public void setDataSource(int dataSource) { this.dataSource = dataSource; }

    public String getExceedType() { return exceedType; }
    public void setExceedType(String exceedType) { this.exceedType = exceedType; }

    public String getTotalWeight() { return totalWeight; }
    public void setTotalWeight(String totalWeight) { this.totalWeight = totalWeight; }

    public String getStatsDate() { return statsDate; }
    public void setStatsDate(String statsDate) { this.statsDate = statsDate; }

    // ================== 辅助方法 (保留原有的业务逻辑) ==================

    public String getLevelDescription() {
        return LEVEL_CRITICAL.equals(level) ? "严重" : "警告";
    }

    public int getLevelColor() {
        return LEVEL_CRITICAL.equals(level) ? 0xFFFF4444 : 0xFFFFA726;
    }

    public boolean isCritical() {
        return LEVEL_CRITICAL.equals(level);
    }

    public boolean isProcessed() {
        return status == STATUS_PROCESSED;
    }

    public boolean isUnprocessed() {
        return status == STATUS_UNPROCESSED;
    }

    public String getStatusDescription() {
        switch (status) {
            case STATUS_PROCESSED:
                return "已处理";
            case STATUS_FALSE_ALARM:
                return "误报"; // 必须是 "误报"，不能是 " 误报 "
            case STATUS_UNPROCESSED:
            default:
                return "未处理"; // 必须是 "未处理"，不能是 "未处理▼"
        }
    }

    public boolean isFalseAlarm() {
        return status == STATUS_FALSE_ALARM;
    }

    public int getStatusColor() {
        switch (status) {
            case STATUS_PROCESSED: return 0xFF43A047;
            case STATUS_FALSE_ALARM: return 0xFFFF9800;
            default: return 0xFFD32F2F;
        }
    }
}