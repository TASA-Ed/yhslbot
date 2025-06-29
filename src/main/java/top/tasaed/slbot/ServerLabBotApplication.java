package top.tasaed.slbot;


import top.tasaed.slbot.util.ServerParserTool;
import top.tasaed.slbot.util.PortCheckResult;
import top.tasaed.slbot.util.PortCheckerUtil;
import cn.daenx.yhchatsdk.common.constant.RecvTypeConstant;
import cn.daenx.yhchatsdk.framework.utils.ApiUtil;
import cn.daenx.yhchatsdk.framework.vo.v1.req.ApiEditMsgReqV1;
import cn.daenx.yhchatsdk.framework.vo.v1.req.ApiSendMsgReqV1;
import cn.daenx.yhchatsdk.framework.vo.v1.ret.ApiEditMsgRetV1;
import cn.daenx.yhchatsdk.framework.vo.v1.ret.ApiSendMsgRetV1;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.util.Util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Objects;

/**
 * 启动类
 *
 * @author DaenMax
 */
@Slf4j
@SpringBootApplication
//"cn.daenx.yhchatsdk" 是core的路径，切勿修改
//"top.tasaed.slbot.plugin" 是当前Bot的插件所在包路径
@ComponentScan({"cn.daenx.yhchatsdk", "top.tasaed.slbot.plugin"})
public class ServerLabBotApplication {

    /**
     * 启动类
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerLabBotApplication.class, args);
        log.info("【CurrentDirectory】{}", currentDirectory(""));
        Connection c;
        Statement stmt;
        boolean success = false;
        String dbPath = ServerLabBotApplication.getDBPath();
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(dbPath);
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT name FROM sqlite_master WHERE type='table'" );
            while ( rs.next() ) {
                String name = rs.getString("name");
                if (Objects.equals(name, "SERVERS")){
                    success = true;
                    break;
                }
            }
            if (!success){
                log.info("SERVERS 表不存在");
                String sql = "CREATE TABLE SERVERS " +
                        "(GROUPID TEXT PRIMARY KEY NOT NULL," +
                        " ID TEXT NOT NULL, " +
                        " KEY TEXT NOT NULL, " +
                        " PORT TEXT);";
                stmt.executeUpdate(sql);
                log.info("创建表 SERVERS");
            }
            stmt.close();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 机器人版本
     */
    public static final String botVersion = "2.0.1";

    /**
     * 获取当前目录的文件名
     * @param fileName 文件名
     * @return 运行目录+文件名
     */
    public static Path currentDirectory(String fileName) {
        return Path.of(System.getProperty("user.dir") + File.separator + fileName);
    }

    /**
     * 获取本机GroupInfo.db地址
     * @return GroupInfo.db地址
     */
    public static String getDBPath() {
        return "jdbc:sqlite:" + currentDirectory("GroupInfo.db");
    }

    /**
     * 获取CPU使用率（百分比）
     * @return 当前CPU使用率（0.0 - 100.0）
     */
    public static double getCpuUsage() {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        // 第一次采样
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 等待 1 秒获取间隔数据
        Util.sleep(1000);

        // 计算 1 秒内的 CPU 使用率
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        return Math.round(cpuUsage * 10) / 10.0; // 保留一位小数
    }

    /**
     * 获取内存使用率（百分比）
     * @return 当前内存使用率（0.0 - 100.0）
     */
    public static double getMemoryUsage() {
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        double memoryUsage = (usedMemory * 100.0) / totalMemory;
        return Math.round(memoryUsage * 10) / 10.0; // 保留一位小数
    }

    /**
     * 处理错误
     * @param err 错误
     * @return 处理后的错误
     */
    public static String getError(String err) {
        if (Objects.equals(err, "Rate limit exceeded")) {
            err = "请求速率限制";
        } else if (Objects.equals(err, "Access denied")) {
            err = "未找到请求的服务器，`Account ID` 或 `API Key` 无效";
        } else if (Objects.equals(err, "IP not verified")) {
            err = "IP 未认证";
        } else if (Objects.equals(err, "ID must be numeric")) {
            err = "`Account ID` 不可为空";
        } else {
            err = "未知错误";
        }
        return err;
    }

    /**
     * 处理私聊消息
     * @param chatId 用户ID
     * @param msg 消息内容
     */
    public static void sendCustomMsgUser(String chatId, String msg) {
        ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                .Markdown(RecvTypeConstant.USER, chatId, msg);
        ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
        log.info("【SendCustomMsgUser】{}", apiSendRetV1);
    }

    /**
     * 处理状态指令
     * @param groupId 群ID
     */
    public static void sendStatus(String groupId) {
        SystemInfo si = new SystemInfo();
        String os = String.valueOf(si.getOperatingSystem());
        try {
            String readme = Files.readString(currentDirectory("status.md"));
            String result = readme.replace("&os;", os)
                    .replace("&version;", botVersion)
                    .replace("&cpu;", getCpuUsage()+"%")
                    .replace("&ram;", getMemoryUsage()+"%")
                    .replace("&edit;", "0");
            ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
            ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
            log.info("【SendStatusMsg】{}", apiSendRetV1);
            String msgId = apiSendRetV1.getData().getMessageInfo().getMsgId();
            for (int i = 1; i < 4; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //编辑消息
                result = readme.replace("&os;", os)
                        .replace("&version;", botVersion)
                        .replace("&cpu;", getCpuUsage()+"%")
                        .replace("&ram;", getMemoryUsage()+"%")
                        .replace("&edit;", String.valueOf(i));
                ApiEditMsgReqV1 editV1 = new ApiEditMsgReqV1()
                        .setMsgId(msgId)
                        .Markdown(RecvTypeConstant.GROUP, groupId, result);
                ApiEditMsgRetV1 apiEditMsgRetV1 = ApiUtil.editMsg(editV1);
                log.info("【EditStatusMsg】{}", apiEditMsgRetV1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理教程指令
     * @param groupId 群ID
     */
    public static void sendHelp(String groupId) {
        try {
            String readme = Files.readString(currentDirectory("help.md"));
            ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                    .Markdown(RecvTypeConstant.GROUP, groupId, readme);
            ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
            log.info("【SendHelpMsg】{}", apiSendRetV1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取人数
     * @param groupId 群号
     */
    public static void sendGetPlayers(String groupId) {
        String dbPath = ServerLabBotApplication.getDBPath();
        String result;
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(dbPath);
            stmt = c.createStatement();
            String readme;
            ResultSet rs = stmt.executeQuery( "SELECT * FROM SERVERS WHERE GROUPID='" + groupId + "';" );
            if (rs.getString("GROUPID")!=null) {
                String accountID=rs.getString("ID");
                String apiKey=rs.getString("KEY");
                String port=rs.getString("PORT");
                HttpURLConnection connection = null;
                boolean netSuccess = false;
                try {
                    URL url = new URL("https://api.scpslgame.com/serverinfo.php?id=" + accountID + "&key=" + apiKey + "&players=true");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Server-Lab/1.0.0");

                    int responseCode = connection.getResponseCode();
                    log.info("【GetPlayersResponseCode】Response Code: {}",responseCode);

                    // 关键修改：统一处理成功/失败的响应流
                    InputStream inputStream;
                    if (responseCode >= 200 && responseCode < 300) { // 扩展成功状态码范围
                        inputStream = connection.getInputStream();
                        netSuccess = true;
                    } else {
                        inputStream = connection.getErrorStream(); // 错误时读取错误流
                    }

                    // 读取响应内容（无论成功或失败）
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    log.info("【GetPlayersResponse】Response: {}",response);

                    if (netSuccess) {
                        // 获取特定端口服务器信息
                        String filteredServers = ServerParserTool.formatServerInfo(String.valueOf(response),port);
                        boolean success = true;
                        if (filteredServers.equals("error1")) {
                            success = false;
                            filteredServers = "机器人内部错误，请联系开发者";
                        } else if (filteredServers.equals("error2")) {
                            success = false;
                            filteredServers = "未找到匹配的服务器信息，请联系群主或管理员修改 服务器端口筛选 选项。";
                        }
                        try {
                            if (success) {
                                readme = Files.readString(ServerLabBotApplication.currentDirectory("getPlayers.md"));
                                result = readme.replace("&players;", filteredServers)
                                        .replace("null","服务器关闭");
                            } else {
                                readme = Files.readString(ServerLabBotApplication.currentDirectory("getErr.md"));
                                result = readme.replace("&err;", filteredServers);
                            }
                            ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                            log.info("【SendGetPlayersMsg】{}", apiSendRetV1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root;
                        try {
                            root = mapper.readTree(String.valueOf(response));
                            String err = root.get("Error").asText();
                            readme = Files.readString(ServerLabBotApplication.currentDirectory("getErr.md"));
                            result = readme.replace("&err;", getError(err));
                            ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                            log.info("【SendGetPlayersMsgErr1】{}", apiSendRetV1);
                        } catch (JsonProcessingException e) {
                            readme = Files.readString(ServerLabBotApplication.currentDirectory("getErr.md"));
                            result = readme.replace("&err;", "机器人内部错误，请联系开发者");
                            ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                            log.info("【SendGetPlayersMsgErr2】{}", apiSendRetV1);
                        }
                    }
                } catch (Exception e) {
                    log.error("【GetPlayers】Exception: {}", e.getMessage());
                } finally {
                    if (connection != null) {
                        connection.disconnect(); // 确保释放连接
                    }
                }
            }
            else {
                try {
                    readme = Files.readString(ServerLabBotApplication.currentDirectory("getErrNoSet.md"));
                    ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                            .Markdown(RecvTypeConstant.GROUP, groupId, readme);
                    ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                    log.info("【SendGetPlayersMsgErr3】{}", apiSendRetV1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            stmt.close();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理机器人设置
     * @param groupId 群号
     */
    public static void sendSetting(String groupId, String accountID, String apiKey, String serverPort) {
        String warn = "";
        String dbPath = getDBPath();
        String result;
        Connection c;
        Statement stmt;
        if (Objects.equals(accountID, "") && Objects.equals(apiKey, "") && Objects.equals(serverPort, "")) {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection(dbPath);
                stmt = c.createStatement();
                String sql = "DELETE from SERVERS where GROUPID='" + groupId + "';";
                stmt.executeUpdate(sql);
                stmt.close();
                c.close();
                try {
                    String readme = Files.readString(currentDirectory("settingDel.md"));
                    ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                            .Markdown(RecvTypeConstant.GROUP, groupId, readme);
                    ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                    log.info("【SendDeleteMsg】{}", apiSendRetV1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException | SQLException e) {
                try {
                    String readme = Files.readString(currentDirectory("settingDelErr.md"));
                    ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                            .Markdown(RecvTypeConstant.GROUP, groupId, readme);
                    ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                    log.error("【SendDeleteMsgErr】{}", apiSendRetV1);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            boolean success = false;
            HttpURLConnection connection = null;
            try {
                String readme = Files.readString(currentDirectory("setting1.md"));
                ApiSendMsgReqV1 reqV1 = new ApiSendMsgReqV1()
                        .Markdown(RecvTypeConstant.GROUP, groupId, readme);
                ApiSendMsgRetV1 apiSendRetV1 = ApiUtil.sendMsg(reqV1);
                log.info("【SendSettingMsg】{}", apiSendRetV1);
                String msgId = apiSendRetV1.getData().getMessageInfo().getMsgId();
                try {
                    URL url = new URL("https://api.scpslgame.com/serverinfo.php?id=" + accountID + "&key=" + apiKey + "&players=true");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Server-Lab/1.0.0");

                    int responseCode = connection.getResponseCode();
                    log.info("【SendSettingResponseCode】Response Code: {}", responseCode);

                    // 关键修改：统一处理成功/失败的响应流
                    InputStream inputStream;
                    if (responseCode >= 200 && responseCode < 300) { // 扩展成功状态码范围
                        inputStream = connection.getInputStream();
                        success = true;
                        readme = Files.readString(currentDirectory("setting2.md"));
                        ApiEditMsgReqV1 editV1 = new ApiEditMsgReqV1()
                                .setMsgId(msgId)
                                .Markdown(RecvTypeConstant.GROUP, groupId, readme);
                        ApiEditMsgRetV1 apiEditMsgRetV1 = ApiUtil.editMsg(editV1);
                        log.info("【EditSetting2Msg】{}", apiEditMsgRetV1);
                    } else {
                        inputStream = connection.getErrorStream(); // 错误时读取错误流
                    }

                    // 读取响应内容（无论成功或失败）
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    log.info("【SendSettingResponse】Response: {}", response);

                    if (success) {
                        try {
                            try {
                                PortCheckResult checkPort = PortCheckerUtil.checkPortsInJson(String.valueOf(response), serverPort);
                                if (!checkPort.allExist()) {
                                    warn = "警告：端口为 `" + checkPort.getMissingPorts() + "` 的服务器不存在，可能导致查询失败。";
                                }
                            } catch (Exception e) {
                                log.error("{}: {}", e.getClass().getName(), e.getMessage());
                            }
                            Class.forName("org.sqlite.JDBC");
                            c = DriverManager.getConnection(dbPath);
                            stmt = c.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT * FROM SERVERS WHERE GROUPID='" + groupId + "';");
                            if (rs.getString("GROUPID") != null) {
                                String sql = "UPDATE SERVERS set KEY='" + apiKey + "' WHERE GROUPID='" + groupId + "';";
                                stmt.executeUpdate(sql);
                                sql = "UPDATE SERVERS set ID='" + accountID + "' WHERE GROUPID='" + groupId + "';";
                                stmt.executeUpdate(sql);
                                sql = "UPDATE SERVERS set PORT='" + serverPort + "' WHERE GROUPID='" + groupId + "';";
                                stmt.executeUpdate(sql);
                            } else {
                                String sql = "INSERT INTO SERVERS (GROUPID,KEY,ID,PORT) " +
                                        "VALUES ('" + groupId + "', '" + apiKey + "', '" + accountID + "', '" + serverPort + "');";
                                stmt.executeUpdate(sql);
                            }
                            stmt.close();
                            c.close();
                            readme = Files.readString(currentDirectory("setting4.md"));
                            result = readme.replace("&warn;", warn);
                            ApiEditMsgReqV1 edit2 = new ApiEditMsgReqV1()
                                    .setMsgId(msgId)
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiEditMsgRetV1 apiEditMsgRet2 = ApiUtil.editMsg(edit2);
                            log.info("【EditSetting4Msg】{}", apiEditMsgRet2);
                        } catch (ClassNotFoundException e) {
                            log.error("{}: {}", e.getClass().getName(), e.getMessage());
                            readme = Files.readString(currentDirectory("setting3.md"));
                            result = readme.replace("&err;", "机器人内部错误，请联系开发者");
                            ApiEditMsgReqV1 editV1 = new ApiEditMsgReqV1()
                                    .setMsgId(msgId)
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiEditMsgRetV1 apiEditMsgRetV1 = ApiUtil.editMsg(editV1);
                            log.error("【EditSetting3sqlMsg】{}", apiEditMsgRetV1);
                        }
                        log.info("【SQLite】Records created successfully");
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root;
                        try {
                            root = mapper.readTree(String.valueOf(response));
                            String err = root.get("Error").asText();
                            readme = Files.readString(currentDirectory("setting3.md"));
                            result = readme.replace("&err;", getError(err));
                            ApiEditMsgReqV1 editV1 = new ApiEditMsgReqV1()
                                    .setMsgId(msgId)
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiEditMsgRetV1 apiEditMsgRetV1 = ApiUtil.editMsg(editV1);
                            log.error("【EditSetting3netMsg】{}", apiEditMsgRetV1);
                        } catch (JsonProcessingException e) {
                            readme = Files.readString(currentDirectory("setting3.md"));
                            result = readme.replace("&err;", "机器人内部错误，请联系开发者");
                            ApiEditMsgReqV1 editV1 = new ApiEditMsgReqV1()
                                    .setMsgId(msgId)
                                    .Markdown(RecvTypeConstant.GROUP, groupId, result);
                            ApiEditMsgRetV1 apiEditMsgRetV1 = ApiUtil.editMsg(editV1);
                            log.error("【EditJSONErrorMsg】{}, {}", apiEditMsgRetV1, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("【SendSetting】Exception: {}", e.getMessage());
                } finally {
                    if (connection != null) {
                        connection.disconnect(); // 确保释放连接
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
