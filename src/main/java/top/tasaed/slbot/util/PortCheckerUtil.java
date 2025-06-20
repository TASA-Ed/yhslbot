package top.tasaed.slbot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class PortCheckerUtil {

    /**
     * 检查JSON中是否存在指定的端口
     * @param json JSON字符串
     * @param portsInput 用户输入的端口（逗号分隔）
     * @return 包含检查结果的对象
     */
    public static PortCheckResult checkPortsInJson(String json, String portsInput) throws Exception {
        // 解析用户输入的端口
        List<Integer> inputPorts = parsePortsInput(portsInput);

        // 从JSON提取所有端口
        Set<Integer> jsonPorts = extractPortsFromJson(json);

        // 检查端口存在性
        List<Integer> existing = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();

        for (int port : inputPorts) {
            if (jsonPorts.contains(port)) {
                existing.add(port);
            } else {
                missing.add(port);
            }
        }

        return new PortCheckResult(existing, missing);
    }

    /**
     * 解析用户输入的端口字符串
     * @param input 逗号分隔的端口字符串
     * @return 整数端口列表
     */
    private static List<Integer> parsePortsInput(String input) {
        List<Integer> ports = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return ports;
        }

        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                int port = Integer.parseInt(part.trim());
                ports.add(port);
            } catch (NumberFormatException e) {
                // 可记录日志或抛出自定义异常
                System.err.println("忽略无效端口: " + part);
            }
        }
        return ports;
    }

    /**
     * 从JSON提取所有端口号
     * @param json JSON字符串
     * @return 端口集合
     */
    private static Set<Integer> extractPortsFromJson(String json) throws Exception {
        Set<Integer> ports = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode servers = root.path("Servers");
        for (JsonNode server : servers) {
            if (server.has("Port") && server.get("Port").isInt()) {
                ports.add(server.get("Port").asInt());
            }
        }
        return ports;
    }
}