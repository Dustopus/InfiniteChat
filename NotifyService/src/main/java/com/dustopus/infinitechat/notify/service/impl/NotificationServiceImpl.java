package com.dustopus.infinitechat.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.notify.mapper.NotificationMapper;
import com.dustopus.infinitechat.notify.model.Notification;
import com.dustopus.infinitechat.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public List<Notification> getNotifications(Long userId, int pageSize, Long lastId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (lastId != null) {
            wrapper.lt(Notification::getId, lastId);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        wrapper.last("LIMIT " + pageSize);
        return notificationMapper.selectList(wrapper);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String content, Long refId) {
        Notification notification = new Notification();
        notification.setId(snowflakeIdGenerator.nextId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setRefId(refId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
