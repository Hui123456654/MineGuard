package com.example.mineguard.alarm.model;

import java.io.Serializable;

/**
 * 报警数据模型
 */
public class AlarmItem implements Serializable {
    public static final int STATUS_UNPROCESSED = 0;  // 未处理
    public static final int STATUS_PROCESSED = 1;    // 已处理
    public static final int STATUS_FALSE_ALARM = 2;  // 误报 (新增)
    public static final String LEVEL_WARNING = "0";  // 警告级别
    public static final String LEVEL_CRITICAL = "1"; // 严重级别

    private int id;               // 报警ID
    private String channel;       // 报警通道
    private String type;          // 报警类型
    private String level;         // 报警等级
    private String algorithm_code;  //算法类型
    private String path;          // 图片相对路径
    private String video_path;    // 视频相对路径
    private String[] video_paths; // 视频路径列表
    private int status;           // 处理状态（0=未处理，1=已处理）
    private Integer camera_id;    // 关联摄像机ID
    private String url;           // 摄像机RTSP流地址
    private String solve_time;    // 解决时间
    private String ip;            // 摄像机IP（camera_id有效时返回）
    private String name;          // 摄像机名称（camera_id有效时返回）
    private String location;      // 摄像机位置（camera_id有效时返回）
    private String flow;          // 摄像机流地址（camera_id有效时返回）
    private String processInfo;   // 保留处理信息

    private int dataSource;         // 数据来源：1=普通报警, 3=四超报警, 6=煤量统计
    private String exceedType;      // [接口3] 报警类型 (height, width, length, weight)
    private String totalWeight;     // [接口6] 总煤量
    private String statsDate;       // [接口6] 统计时间段


    public AlarmItem() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVideo_path() {
        return video_path;
    }

    public void setVideo_path(String video_path) {
        this.video_path = video_path;
    }

    public String[] getVideo_paths() {
        return video_paths;
    }

    public void setVideo_paths(String[] video_paths) {
        this.video_paths = video_paths;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getCamera_id() {
        return camera_id;
    }

    public void setCamera_id(Integer camera_id) {
        this.camera_id = camera_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSolve_time() {
        return solve_time;
    }

    public void setSolve_time(String solve_time) {
        this.solve_time = solve_time;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAlgorithm_code() {
        return algorithm_code;
    }

    public void setAlgorithm_code(String algorithm_code) {
        this.algorithm_code = algorithm_code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getProcessInfo() {
        return processInfo;
    }

    public void setProcessInfo(String processInfo) {
        this.processInfo = processInfo;
    }

    /**
     * 获取级别描述
     */
    public String getLevelDescription() {
        return LEVEL_CRITICAL.equals(level) ? "严重" : "警告";
    }

    /**
     * 获取级别颜色
     */
    public int getLevelColor() {
        return LEVEL_CRITICAL.equals(level) ? 0xFFFF4444 : 0xFFFFA726; // 红色或橙色
    }

    /**
     * 是否为严重报警
     */
    public boolean isCritical() {
        return LEVEL_CRITICAL.equals(level);
    }

    /**
     * 是否已处理
     */
    public boolean isProcessed() {
        return status == STATUS_PROCESSED;
    }

    /**
     * 是否未处理
     */
    public boolean isUnprocessed() {
        return status == STATUS_UNPROCESSED;
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return status == STATUS_PROCESSED ? "已处理" : "未处理";
    }
    /**
     * 是否为误报
     */
    public boolean isFalseAlarm() {
        return status == STATUS_FALSE_ALARM;
    }

    public int getDataSource() { return dataSource; }
    public void setDataSource(int dataSource) { this.dataSource = dataSource; }

    public String getExceedType() { return exceedType; }
    public void setExceedType(String exceedType) { this.exceedType = exceedType; }

    public String getTotalWeight() { return totalWeight; }
    public void setTotalWeight(String totalWeight) { this.totalWeight = totalWeight; }

    public String getStatsDate() { return statsDate; }
    public void setStatsDate(String statsDate) { this.statsDate = statsDate; }
    /**
     * 获取状态对应的颜色 (将颜色逻辑封装在 Model 层)
     */
    public int getStatusColor() {
        switch (status) {
            case STATUS_PROCESSED:
                return 0xFF43A047; // 绿色
            case STATUS_FALSE_ALARM:
                return 0xFFFF9800; // 橙色
            default:
                return 0xFFD32F2F; // 红色
        }
    }
}