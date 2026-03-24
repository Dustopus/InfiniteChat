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
                // Here we just log; actual group broadcast is handled by GroupMessageService
                log.debug("Group message {} received, will be broadcast via pub/sub", message.getMessageId());
            }
        } catch (Exception e) {
            log.error("Failed to push message to WebSocket", e);
        }
    }

    @KafkaListener(topics = MessageConstants.KAFKA_TOPIC_NOTIFY, groupId = "rtc-notify-group")
    public void onNotification(String notifyJson) {
        try {
            WebSocketMessage wsMsg = WebSocketMessage.of("notification", notifyJson);
            String json = objectMapper.writeValueAsString(wsMsg);
            // Notification routing is handled by NotifyService
            log.debug("Notification received: {}", notifyJson);
        } catch (Exception e) {
            log.error("Failed to handle notification", e);
        }
    }

    private void pushToUser(Long userId, String json) {
        boolean sent = channelManager.sendToUser(userId, json);
        if (!sent) {
            log.debug("User {} is offline, message queued for offline delivery", userId);
        }
    }
}
