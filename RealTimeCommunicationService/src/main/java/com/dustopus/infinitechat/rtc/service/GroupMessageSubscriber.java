package com.dustopus.infinitechat.rtc.service;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.dto.message.WebSocketMessage;
import com.dustopus.infinitechat.rtc.manager.ChannelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageSubscriber {

    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                         MessageListener groupMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(groupMessageListener,
                new ChannelTopic(MessageConstants.REDIS_CHANNEL_MESSAGE));
        return container;
    }

    @Bean
    public MessageListener groupMessageListener() {
        return (message, pattern) -> {
            try {
                String body = new String(message.getBody());
                // Format: "userId1,userId2,...|messageJson"
                int sep = body.indexOf('|');
                if (sep == -1) return;

                String[] userIds = body.substring(0, sep).split(",");
                String json = body.substring(sep + 1);

                for (String uid : userIds) {
                    try {
                        Long userId = Long.parseLong(uid.trim());
                        channelManager.sendToUser(userId, json);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid userId in group broadcast: {}", uid);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to handle group message from Redis pub/sub", e);
            }
        };
    }
}
