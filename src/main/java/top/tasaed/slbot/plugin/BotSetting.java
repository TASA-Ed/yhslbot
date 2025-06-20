package top.tasaed.slbot.plugin;

import top.tasaed.slbot.ServerLabBotApplication;
import cn.daenx.yhchatsdk.framework.eventInterface.EventBotSetting;
import cn.daenx.yhchatsdk.framework.vo.EventMsgVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;


/**
 * 机器人设置事件插件
 *
 * @author DaenMax
 */
@Slf4j
@Service
@Order(1)//相同事件插件中的优先级，越小越优先
public class BotSetting implements EventBotSetting {
    /**
     * 返回-1，后面的实现类将不再执行
     * 返回0，后面的实现类继续执行
     *
     */
    @Override
    public Integer handle(EventMsgVo eventMsgVo) {
        String chatId = eventMsgVo.getEvent().getChatId();
        String groupId = eventMsgVo.getEvent().getGroupId();
        String groupName = eventMsgVo.getEvent().getGroupName();
        //json字符串，自行解析
        String settingJson = eventMsgVo.getEvent().getSettingJson();
        log.info("【机器人设置事件】群号[{}]（{}），机器人ID[{}]，表单内容：{}", groupId, groupName, chatId, settingJson);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(settingJson);
            String apiKey = root.get("tsmnpc").get("value").asText();
            String accountID = root.get("jtfzim").get("value").asText();
            String serverPort = root.get("bqdgac").get("value").asText();
            ServerLabBotApplication.sendSetting(groupId, accountID, apiKey, serverPort);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //返回-1则不再投递后面的同事件插件
        //返回0则继续投递给后面的同事件插件处理
        return 0;
    }
}
