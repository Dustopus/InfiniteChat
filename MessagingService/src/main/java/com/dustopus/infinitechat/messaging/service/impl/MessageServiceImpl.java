package com.dustopus.infinitechat.messaging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.common.dto.message.WebSocketMessage;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.messaging.mapper.MessageMapper;
import com.dustopus.infinitechat.messaging.model.Message;
import com.dustopus.infinitechat.messaging.service.MessageService;
import com.dustopus.infinitechat.messaging.vo.SendMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageVO vo) {
        // Validate
        if (MessageConstants.SINGLE_CHAT.equals(vo.getChatType())) {
            if (vo.getReceiverId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "单聊必须指定接收者");
            }
        } else if (MessageConstants.GROUP_CHAT.equals(vo.getChatType())) {
            if (vo.getGroupId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "群聊必须指定群组ID");
            }
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不支持的聊天类型");
        }

        // Create message
        Message message = new Message();
        message.setMessageId(snowflakeIdGenerator.nextId());
        message.setSenderId(senderId);
        message.setReceiverId(vo.getReceiverId());
        message.setGroupId(vo.getGroupId());
        message.setChatType(vo.getChatType());
        message.setMsgType(vo.getMsgType());
        message.setContent(vo.getContent());
        message.setExtra(vo.getExtra());
        message.setReplyTo(vo.getReplyTo());
        message.setStatus(1);
        messageMapper.insert(message);

        MessageDTO dto = toDTO(message);

        // Send to Kafka for async processing
        try {
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_MESSAGE, json);

            // If group chat, also publish to Redis for broadcasting
            if (MessageConstants.GROUP_CHAT.equals(vo.getChatType())) {
                publishGroupMessage(vo.getGroupId(), json);
            }
        } catch (Exception e) {
            log.error("Failed to send message to Kafka", e);
        }

        return dto;
    }

    @Override
    public List<MessageDTO> getChatHistory(Long userId, Long targetId, String chatType,
                                            Long lastMessageId, int pageSize) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();

        if (MessageConstants.SINGLE_CHAT.equals(chatType)) {
            // Single chat: get messages between two users
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Message::getSenderId, userId).eq(Message::getReceiverId, targetId))
                    .or(w2 -> w2.eq(Message::getSenderId, targetId).eq(Message::getReceiverId, userId)))
                    .eq(Message::getStatus, 1);
        } else {
            // Group chat
            wrapper.eq(Message::getGroupId, targetId)
                    .eq(Message::getStatus, 1);
        }

        if (lastMessageId != null) {
            wrapper.lt(Message::getMessageId, lastMessageId);
        }

        wrapper.orderByDesc(Message::getMessageId);
        wrapper.last("LIMIT " + pageSize);

        List<Message> messages = messageMapper.selectList(wrapper);
        List<MessageDTO> result = new ArrayList<>();
        for (Message m : messages) {
            result.add(toDTO(m));
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只能撤回自己发送的消息");
        }
        message.setStatus(2);
        messageMapper.updateById(message);

        // Notify via Kafka
        try {
            MessageDTO dto = toDTO(message);
            dto.setContent("[消息已撤回]");
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_MESSAGE, json);
        } catch (Exception e) {
            log.error("Failed to send recall notification", e);
        }
    }

    @Override
    public List<?> getRecentChats(Long userId) {
        // 查询该用户最近的会话：按最后一条消息时间倒序
        // Get single chat messages where user is sender or receiver
        List<Message> singleMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .and(w -> w
                                .eq(Message::getSenderId, userId)
                                .or()
                                .eq(Message::getReceiverId, userId))
                        .eq(Message::getChatType, MessageConstants.SINGLE_CHAT)
                        .eq(Message::getStatus, 1)
                        .orderByDesc(Message::getMessageId)
                        .last("LIMIT 50"));

        // Get group chat messages for groups the user belongs to
        // First get user's group IDs
        List<Long> groupIds = new ArrayList<>();
        try {
            String key = "group:user:" + userId;
            var userGroups = stringRedisTemplate.opsForSet().members(key);
            if (userGroups != null) {
                for (String gid : userGroups) {
                    groupIds.add(Long.parseLong(gid));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get user groups from Redis", e);
        }

        List<Message> groupMessages = new ArrayList<>();
        if (!groupIds.isEmpty()) {
            groupMessages = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .in(Message::getGroupId, groupIds)
                            .eq(Message::getChatType, MessageConstants.GROUP_CHAT)
                            .eq(Message::getStatus, 1)
                            .orderByDesc(Message::getMessageId)
                            .last("LIMIT 50"));
        }

        // Merge and deduplicate
        List<Message> allMessages = new ArrayList<>(singleMessages);
        allMessages.addAll(groupMessages);
        allMessages.sort((a, b) -> Long.compare(b.getMessageId(), a.getMessageId()));

        // 去重：每个 targetId 只保留最新一条
        java.util.LinkedHashMap<String, Map<String, Object>> chatMap = new java.util.LinkedHashMap<>();
        for (Message msg : allMessages) {
            String key;
            Long targetId;
            String chatType = msg.getChatType();

            if (MessageConstants.SINGLE_CHAT.equals(chatType)) {
                targetId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
                if (targetId == null) continue;
                key = "single_" + targetId;
            } else {
                targetId = msg.getGroupId();
                if (targetId == null) continue;
                key = "group_" + targetId;
            }

            if (!chatMap.containsKey(key)) {
                Map<String, Object> chat = new HashMap<>();
                chat.put("chatType", chatType);
                chat.put("targetId", targetId);
                chat.put("lastMessage", msg.getContent());
                chat.put("lastMessageId", msg.getMessageId());
                chat.put("lastTime", msg.getCreatedAt());
                chatMap.put(key, chat);
            }
        }

        return new ArrayList<>(chatMap.values());
    }

    /**
     * Publish group message to Redis for broadcasting to all group members
     */
    private void publishGroupMessage(Long groupId, String messageJson) {
        try {
            // Get group member IDs from Redis cache
            String key = "group:members:" + groupId;
            var members = stringRedisTemplate.opsForSet().members(key);
            if (members != null && !members.isEmpty()) {
                String userIds = String.join(",", members);
                String payload = userIds + "|" + messageJson;
                stringRedisTemplate.convertAndSend(MessageConstants.REDIS_CHANNEL_MESSAGE, payload);
            }
        } catch (Exception e) {
            log.error("Failed to publish group message", e);
        }
    }

    private MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setGroupId(message.getGroupId());
        dto.setChatType(message.getChatType());
        dto.setMsgType(message.getMsgType());
        dto.setContent(message.getContent());
        dto.setExtra(message.getExtra());
        dto.setTimestamp(message.getCreatedAt() != null ? message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis());
        dto.setReplyTo(message.getReplyTo());
        return dto;
    }
}
