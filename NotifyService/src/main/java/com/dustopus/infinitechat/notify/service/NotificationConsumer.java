package com.dustopus.infinitechat.notify.service;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
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
                    pushToUser(toId, "friend_request", fromId);
                }
            } else if ("friend_accepted".equals(type)) {
                // ContactService sends: userId=acceptor, friendId=original applicant
                // We need to notify the original applicant (friendId)
                Long acceptorId = node.has("userId") ? node.get("userId").asLong() : null;
                Long applicantId = node.has("friendId") ? node.get("friendId").asLong() : null;
                if (acceptorId != null && applicantId != null) {
                    notificationService.createNotification(applicantId, "friend",
                            "你与用户 " + acceptorId + " 已成为好友", acceptorId);
                    pushToUser(applicantId, "friend_accepted", acceptorId);
                }
            } else {
                log.info("Received notification: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process notification: {}", message, e);
        }
    }

    /**
     * 将通知推送到 RTC 服务，由其通过 WebSocket 下发给目标用户
     */
    private void pushToUser(Long userId, String notifType, Long refId) {
        try {
            Map<String, Object> push = new HashMap<>();
            push.put("type", "notification");
            push.put("userId", userId);
            push.put("notifType", notifType);
            push.put("refId", refId);
            kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_MESSAGE, objectMapper.writeValueAsString(push));
        } catch (Exception e) {
            log.error("Failed to push notification to user {}", userId, e);
        }
    }
}
