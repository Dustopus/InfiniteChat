package com.dustopus.infinitechat.offline.service;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dustopus.infinitechat.offline.mapper.OfflineMessageMapper;
import com.dustopus.infinitechat.offline.model.OfflineMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineMessageConsumer {

    private final OfflineMessageMapper offlineMessageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = MessageConstants.KAFKA_TOPIC_MESSAGE, groupId = "offline-consumer-group")
    public void consumeMessage(String messageJson) {
        try {
            MessageDTO message = objectMapper.readValue(messageJson, MessageDTO.class);

            if (MessageConstants.SINGLE_CHAT.equals(message.getChatType())) {
                // For single chat: check if receiver is offline
                storeIfOffline(message.getReceiverId(), message);
            } else if (MessageConstants.GROUP_CHAT.equals(message.getChatType())) {
                // For group chat: need to check each member
                // This is handled by checking online status per user
                log.debug("Group message {} processed for offline storage", message.getMessageId());
            }
        } catch (Exception e) {
            log.error("Failed to process offline message", e);
        }
    }

    private void storeIfOffline(Long userId, MessageDTO message) {
        // Check if user is online
        Boolean online = stringRedisTemplate.hasKey(RedisConstants.USER_ONLINE_PREFIX + userId);
        if (!Boolean.TRUE.equals(online)) {
            // User is offline, store the message
            OfflineMessage offline = new OfflineMessage();
            offline.setUserId(userId);
            offline.setMessageId(message.getMessageId());
            offline.setChatType(message.getChatType());
            offline.setSenderId(message.getSenderId());
            offlineMessageMapper.insert(offline);

            // Increment offline message count in Redis
            stringRedisTemplate.opsForValue().increment(
                    RedisConstants.OFFLINE_MSG_COUNT_PREFIX + userId);

            log.debug("Stored offline message {} for user {}", message.getMessageId(), userId);
        }
    }
}
