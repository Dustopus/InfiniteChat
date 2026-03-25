package com.dustopus.infinitechat.rtc.service;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.common.dto.message.WebSocketMessage;
import com.dustopus.infinitechat.rtc.manager.ChannelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushService {

    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = MessageConstants.KAFKA_TOPIC_MESSAGE, groupId = "rtc-push-group")
    public void onMessage(String messageJson) {
        try {
            // Check if this is a notification push (has "notifType" field)
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(messageJson);
            if (node.has("notifType")) {
                // This is a notification — push to the target user
                Long userId = node.get("userId").asLong();
                WebSocketMessage wsMsg = WebSocketMessage.of("notification", node);
                pushToUser(userId, objectMapper.writeValueAsString(wsMsg));
                return;
            }

            MessageDTO message = objectMapper.readValue(messageJson, MessageDTO.class);
            WebSocketMessage wsMsg = WebSocketMessage.of("message", message);
            String json = objectMapper.writeValueAsString(wsMsg);

            if (MessageConstants.SINGLE_CHAT.equals(message.getChatType())) {
                // Single chat: push to receiver
                pushToUser(message.getReceiverId(), json);
                // Also push confirmation to sender
                pushToUser(message.getSenderId(), json);
            } else if (MessageConstants.GROUP_CHAT.equals(message.getChatType())) {
                // Group chat: push to all members via Redis pub/sub (handled separately)
                log.debug("Group message {} received, will be broadcast via pub/sub", message.getMessageId());
            }
        } catch (Exception e) {
            log.error("Failed to push message to WebSocket", e);
        }
    }

    /**
     * Notification push is handled via KAFKA_TOPIC_MESSAGE by NotifyService.
     * NotifyService consumes from KAFKA_TOPIC_NOTIFY, creates DB notification,
     * then re-publishes to KAFKA_TOPIC_MESSAGE for RTC to push.
     * No direct consumption of KAFKA_TOPIC_NOTIFY here to avoid duplicate pushes.
     */

    private void pushToUser(Long userId, String json) {
        boolean sent = channelManager.sendToUser(userId, json);
        if (!sent) {
            log.debug("User {} is offline, message queued for offline delivery", userId);
        }
    }
}
