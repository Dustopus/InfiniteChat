package com.dustopus.infinitechat.notify.service;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = MessageConstants.KAFKA_TOPIC_NOTIFY, groupId = "notify-consumer-group")
    public void consumeNotification(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String type = node.has("type") ? node.get("type").asText() : "system";

            if ("friend_request".equals(type)) {
                Long toId = node.has("toId") ? node.get("toId").asLong() : null;
                Long fromId = node.has("fromId") ? node.get("fromId").asLong() : null;
                if (toId != null && fromId != null) {
                    notificationService.createNotification(toId, "friend",
                            "用户 " + fromId + " 请求添加你为好友", fromId);
                }
            } else if ("friend_accepted".equals(type)) {
                Long fromId = node.has("fromId") ? node.get("fromId").asLong() : null;
                Long friendId = node.has("friendId") ? node.get("friendId").asLong() : null;
                if (fromId != null && friendId != null) {
                    notificationService.createNotification(friendId, "friend",
                            "你与用户 " + fromId + " 已成为好友", fromId);
                }
            } else {
                log.info("Received notification: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process notification: {}", message, e);
        }
    }
}
