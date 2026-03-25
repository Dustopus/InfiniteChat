package com.dustopus.infinitechat.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.contact.ContactDTO;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.common.dto.user.UserDTO;
import com.dustopus.infinitechat.contact.mapper.ContactMapper;
import com.dustopus.infinitechat.contact.mapper.FriendRequestMapper;
import com.dustopus.infinitechat.contact.mapper.UserSearchMapper;
import com.dustopus.infinitechat.contact.model.Contact;
import com.dustopus.infinitechat.contact.model.FriendRequest;
import com.dustopus.infinitechat.contact.service.ContactService;
import com.dustopus.infinitechat.contact.vo.FriendApplyRequest;
import com.dustopus.infinitechat.contact.vo.FriendHandleRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserSearchMapper userSearchMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void applyFriend(Long userId, FriendApplyRequest request) {
        Long toId = request.getToId();
        if (userId.equals(toId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能添加自己为好友");
        }

        // Check if already friends
        Contact existing = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getFriendId, toId));
        if (existing != null) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
        }

        // Check if already has pending request
        FriendRequest pending = friendRequestMapper.selectOne(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getFromId, userId)
                        .eq(FriendRequest::getToId, toId)
                        .eq(FriendRequest::getStatus, 0));
        if (pending != null) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
        }

        // Create friend request
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(snowflakeIdGenerator.nextId());
        friendRequest.setFromId(userId);
        friendRequest.setToId(toId);
        friendRequest.setMessage(request.getMessage());
        friendRequest.setStatus(0);
        friendRequest.setCreatedAt(LocalDateTime.now());
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.insert(friendRequest);

        // Send real-time notification via Kafka
        try {
            Map<String, Object> notify = new HashMap<>();
            notify.put("type", "friend_request");
            notify.put("fromId", userId);
            notify.put("toId", toId);
            notify.put("requestId", friendRequest.getId());
            notify.put("message", request.getMessage());
            kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_NOTIFY, objectMapper.writeValueAsString(notify));
        } catch (JsonProcessingException e) {
            log.error("Failed to send friend request notification", e);
        }

        // Invalidate contact list cache
        stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + userId);
        stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + toId);
    }

    @Override
    @Transactional
    public void handleFriendRequest(Long userId, FriendHandleRequest request) {
        FriendRequest friendRequest = friendRequestMapper.selectById(request.getRequestId());
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "好友申请不存在");
        }
        if (!friendRequest.getToId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (friendRequest.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该申请已处理");
        }

        // Update request status
        friendRequest.setStatus(request.getAction());
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.updateById(friendRequest);

        if (request.getAction() == 1) {
            // Agree: create bidirectional contact
            createContact(friendRequest.getFromId(), friendRequest.getToId());
            createContact(friendRequest.getToId(), friendRequest.getFromId());

            // Invalidate cache
            stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + friendRequest.getFromId());
            stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + friendRequest.getToId());

            // Send notification
            try {
                Map<String, Object> notify = new HashMap<>();
                notify.put("type", "friend_accepted");
                notify.put("userId", userId);
                notify.put("friendId", friendRequest.getFromId());
                kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_NOTIFY, objectMapper.writeValueAsString(notify));
            } catch (JsonProcessingException e) {
                log.error("Failed to send friend accepted notification", e);
            }
        }
    }

    private void createContact(Long userId, Long friendId) {
        Contact contact = new Contact();
        contact.setId(snowflakeIdGenerator.nextId());
        contact.setUserId(userId);
        contact.setFriendId(friendId);
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        contactMapper.insert(contact);
    }

    @Override
    public List<ContactDTO> getContactList(Long userId) {
        // Try cache first
        String cacheKey = RedisConstants.CONTACT_LIST_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ContactDTO.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached contact list", e);
            }
        }

        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>().eq(Contact::getUserId, userId));

        List<ContactDTO> result = contacts.stream().map(c -> {
            ContactDTO dto = new ContactDTO();
            dto.setContactId(c.getId());
            dto.setUserId(c.getUserId());
            dto.setFriendId(c.getFriendId());
            dto.setRemark(c.getRemark());
            return dto;
        }).collect(Collectors.toList());

        // Cache for 5 minutes
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 300, java.util.concurrent.TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache contact list", e);
        }

        return result;
    }

    @Override
    public List<Object> getFriendRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getToId, userId)
                        .eq(FriendRequest::getStatus, 0)
                        .orderByDesc(FriendRequest::getCreatedAt));
        return requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", r.getId());
            map.put("fromId", r.getFromId());
            map.put("message", r.getMessage());
            map.put("status", r.getStatus());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        contactMapper.delete(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getFriendId, friendId));
        contactMapper.delete(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, friendId)
                        .eq(Contact::getFriendId, userId));

        stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + userId);
        stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + friendId);
    }

    @Override
    public void updateRemark(Long userId, Long friendId, String remark) {
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getFriendId, friendId));
        if (contact == null) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_FOUND);
        }
        contact.setRemark(remark);
        contact.setUpdatedAt(LocalDateTime.now());
        contactMapper.updateById(contact);
        stringRedisTemplate.delete(RedisConstants.CONTACT_LIST_PREFIX + userId);
    }

    @Override
    public List<UserDTO> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return userSearchMapper.searchUsers(keyword.trim());
    }
}
