package com.example.mineguard.alarm; // 注意包名，如果你的文件在 dialog 包下，请改为 package com.example.mineguard.alarm.dialog;

import android.content.Context;
import android.util.Log;
import com.example.mineguard.alarm.model.AlarmItem;
import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地服务器：用于接收相机主动推送过来的报警信息
 * 已支持多接口路径解析 (普通/四超/煤量)
 */
public class LocalServer extends NanoHTTPD {

    private final Context context;
    private OnAlarmReceivedListener listener;

    // 1. 定义三个不同的接口路径
    public static final String URI_ALARM_1 = "/api/recv/alarmInfo";    // 接口1：普通报警
    public static final String URI_ALARM_3 = "/api/recv/fourLimit";    // 接口3：四超报警
    public static final String URI_COAL_6  = "/api/recv/coalWeight";   // 接口6：煤量统计

    // 回调接口，通知界面刷新
    public interface OnAlarmReceivedListener {
        void onAlarmReceived(AlarmItem alarmItem);
    }

    public LocalServer(int port, Context context) {
        super(port);
        this.context = context;
    }

    public void setListener(OnAlarmReceivedListener listener) {
        this.listener = listener;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // 2. 允许这三个路径通过，其他的拦截
        if (!uri.equals(URI_ALARM_1) && !uri.equals(URI_ALARM_3) && !uri.equals(URI_COAL_6)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }

        try {
            // 3. 获取 Content-Length
            String lenStr = session.getHeaders().get("content-length");
            int contentLength = 0;
            if (lenStr != null) {
                try {
                    contentLength = Integer.parseInt(lenStr);
                } catch (NumberFormatException e) {
                    Log.w("LocalServer", "Content-Length 格式错误");
                }
            }

            if (contentLength <= 0) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\":200, \"msg\":\"empty_body\"}");
            }

            // 4. 获取边界 (Boundary)
            String contentType = session.getHeaders().get("content-type");
            if (contentType == null || !contentType.contains("boundary=")) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing boundary");
            }
            String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
            if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                boundary = boundary.substring(1, boundary.length() - 1);
            }
            byte[] boundaryBytes = ("--" + boundary).getBytes();

            // 5. 读取数据流
            InputStream inputStream = session.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int totalRead = 0;

            while (totalRead < contentLength) {
                int remaining = contentLength - totalRead;
                int readSize = Math.min(data.length, remaining);
                int nRead = inputStream.read(data, 0, readSize);
                if (nRead == -1) break;
                buffer.write(data, 0, nRead);
                totalRead += nRead;
            }

            byte[] payload = buffer.toByteArray();
            Log.i("LocalServer", "✅ 数据接收完成，长度: " + payload.length);

            // 6. 开始解析参数
            AlarmItem item = new AlarmItem();

            // 标记数据来源
            if (uri.equals(URI_ALARM_1)) item.setDataSource(1);
            else if (uri.equals(URI_ALARM_3)) item.setDataSource(3);
            else if (uri.equals(URI_COAL_6)) item.setDataSource(6);

            // 获取 IP
            String remoteIp = session.getHeaders().get("remote-addr");
            if (remoteIp == null) remoteIp = session.getHeaders().get("http-client-ip");
            item.setIp(remoteIp); // 使用 setIp

            Map<String, String> debugParams = new HashMap<>();

            // 7. 解析 Multipart Body (寻找分隔符)
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < payload.length - boundaryBytes.length; i++) {
                boolean match = true;
                for (int j = 0; j < boundaryBytes.length; j++) {
                    if (payload[i + j] != boundaryBytes[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    positions.add(i);
                    i += boundaryBytes.length - 1;
                }
            }

            // 8. 遍历每个 Part
            for (int i = 0; i < positions.size() - 1; i++) {
                int start = positions.get(i) + boundaryBytes.length;
                int end = positions.get(i + 1);

                if (end > start + 2 && payload[end - 2] == 13 && payload[end - 1] == 10) {
                    end -= 2;
                }

                int splitIndex = -1;
                for (int k = start; k < end - 4; k++) {
                    if (payload[k] == 13 && payload[k+1] == 10 && payload[k+2] == 13 && payload[k+3] == 10) {
                        splitIndex = k;
                        break;
                    }
                }

                if (splitIndex != -1) {
                    // === 这里定义了报错缺失的变量 ===
                    String headers = new String(payload, start, splitIndex - start);
                    int bodyStart = splitIndex + 4;
                    int bodyLen = end - bodyStart;

                    String name = getHeaderValue(headers, "name");
                    String filename = getHeaderValue(headers, "filename");

                    if (name != null) {
                        if (filename != null) {
                            // 保存文件
                            if (bodyLen > 0) {
                                String extension = ".jpg";
                                if ("alarm_video".equals(name) || filename.endsWith(".mp4")) {
                                    extension = ".mp4";
                                }
                                String savedPath = saveByteFile(payload, bodyStart, bodyLen, "ALARM_" + System.currentTimeMillis(), extension);
                                item.setPath(savedPath);
                            }
                        } else {
                            // 解析文本
                            String value = new String(payload, bodyStart, bodyLen, "UTF-8").trim();
                            debugParams.put(name, value);

                            // === 核心 Switch 逻辑 ===
                            switch (name) {
                                // === 公共字段 ===
                                case "extend": item.setIp(value); break; // 借用 ip 字段存 extend
                                case "alarm_info_id":
                                    try { item.setId(Integer.parseInt(value)); } catch (Exception e){}
                                    break;

                                // === 接口 1 (普通报警) ===
                                case "detect_target": item.setType(value); break;
                                case "occur_time": item.setSolve_time(value); break;
                                case "algorithm_code": item.setAlgorithm_code(value); break;

                                // === 接口 3 (四超报警) ===
                                case "exceed_type":
                                    item.setExceedType(value);
                                    item.setType("四超: " + value);
                                    break;
                                case "add_time":
                                    item.setSolve_time(value); // 复用 solve_time
                                    break;

                                // === 接口 6 (煤量统计) ===
                                case "total_weight":
                                    item.setTotalWeight(value);
                                    item.setType("煤量统计");
                                    break;
                                case "add_date":
                                    item.setStatsDate(value);
                                    break;
                            }
                        }
                    }
                }
            }

            Log.i("LocalServer", "解析参数: " + debugParams.toString());

            // 特殊处理：如果是煤量统计，设置名称
            if (uri.equals(URI_COAL_6)) {
                item.setName(item.getTotalWeight() + " 吨");
            } else if (uri.equals(URI_ALARM_3)) {
                item.setName("越限报警");
            }

            // 通知 UI
            item.setStatus(0);
            item.setLevel(AlarmItem.LEVEL_WARNING);
            if (listener != null) {
                listener.onAlarmReceived(item);
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\":200, \"msg\":\"success\"}");

        } catch (Exception e) {
            Log.e("LocalServer", "解析崩溃", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: " + e.getMessage());
        }
    }

    // --- 辅助方法 ---
    private String getHeaderValue(String headers, String key) {
        String token = key + "=\"";
        int start = headers.indexOf(token);
        if (start != -1) {
            start += token.length();
            int end = headers.indexOf("\"", start);
            if (end != -1) {
                return headers.substring(start, end);
            }
        }
        return null;
    }

    private String saveByteFile(byte[] data, int offset, int len, String prefix, String extension) {
        File dir = context.getExternalFilesDir("alarms");
        if (!dir.exists()) dir.mkdirs();
        File dst = new File(dir, prefix + extension);
        try (FileOutputStream fos = new FileOutputStream(dst)) {
            fos.write(data, offset, len);
            return dst.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}