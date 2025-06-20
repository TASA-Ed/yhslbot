package top.tasaed.slbot.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ServerParserTool {

    // 内部模型类
    private static class ApiResponse {
        @JsonProperty("Success")
        private boolean success;
        @JsonProperty("Cooldown")
        private int cooldown;
        @JsonProperty("Servers")
        private List<GameServer> servers;

        public List<GameServer> getServers() {
            return servers != null ? servers : Collections.emptyList();
        }
    }


    private static class GameServer {
        @JsonProperty("ID")
        private int id;
        @Getter
        @JsonProperty("Port")
        private int port;
        @Getter
        @JsonProperty("Players")
        private String players;

    }

    /**
     * 解析JSON并格式化输出服务器信息
     *
     * @param json        JSON字符串
     * @param serverPorts 端口字符串（多个端口用英文逗号分隔，可为空）
     * @return 格式化后的服务器信息字符串
     */
    public static String formatServerInfo(String json, String serverPorts) {
        try {
            // 1. 解析JSON
            ObjectMapper mapper = new ObjectMapper();
            ApiResponse response = mapper.readValue(json, ApiResponse.class);

            // 2. 处理端口参数
            Set<Integer> targetPorts = parsePorts(serverPorts);

            // 3. 筛选服务器
            List<GameServer> filteredServers = response.getServers().stream()
                    .filter(server -> targetPorts.isEmpty() || targetPorts.contains(server.getPort()))
                    .collect(Collectors.toList());

            // 4. 格式化输出
            return formatOutput(filteredServers);

        } catch (Exception e) {
            log.error(e.getMessage());
            return "error1";
        }
    }

    /**
     * 格式化服务器信息输出
     *
     * @param servers 服务器列表
     * @return 格式化后的字符串
     */
    private static String formatOutput(List<GameServer> servers) {
        if (servers.isEmpty()) {
            return "error2";
        }

        StringBuilder sb = new StringBuilder();

        // 使用IntStream处理索引
        IntStream.range(0, servers.size())
                .forEach(i -> {
                    GameServer server = servers.get(i);
                    sb.append("- 服务器 ").append(i).append(":\n")
                            .append("  - 端口: ").append(server.getPort()).append("\n")
                            .append("  - 当前人数: ").append(server.getPlayers()).append("\n");
                });

        return sb.toString();
    }

    /**
     * 解析端口字符串
     */
    private static Set<Integer> parsePorts(String portStr) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> ports = new HashSet<>();
        String[] portArray = portStr.split(",");

        for (String port : portArray) {
            try {
                ports.add(Integer.parseInt(port.trim()));
            } catch (NumberFormatException e) {
                // 忽略无效端口
            }
        }

        return ports;
    }

}