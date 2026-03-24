package com.dustopus.infinitechat.offline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.offline.mapper.OfflineMessageMapper;
import com.dustopus.infinitechat.offline.model.OfflineMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineMessageService {

    private final OfflineMessageMapper offlineMessageMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取离线消息数量
     */
    public Long getOfflineCount(Long userId) {
        String count = stringRedisTemplate.opsForValue().get(
                RedisConstants.OFFLINE_MSG_COUNT_PREFIX + userId);
        return count != null ? Long.parseLong(count) : 0L;
    }

    /**
     * 拉取离线消息（用户上线后调用）
     */
    @Transactional
    public List<OfflineMessage> pullOfflineMessages(Long userId) {
        List<OfflineMessage> messages = offlineMessageMapper.selectList(
                new LambdaQueryWrapper<OfflineMessage>()
                        .eq(OfflineMessage::getUserId, userId)
                        .orderByAsc(OfflineMessage::getCreatedAt));

        // Clear offline messages after pull
        if (!messages.isEmpty()) {
            offlineMessageMapper.delete(
                    new LambdaQueryWrapper<OfflineMessage>()
                            .eq(OfflineMessage::getUserId, userId));
            // Reset count
            stringRedisTemplate.delete(RedisConstants.OFFLINE_MSG_COUNT_PREFIX + userId);
        }

        return messages;
    }

    /**
     * 标记离线消息已读（清除计数但保留记录）
     */
    public void markAsRead(Long userId) {
        stringRedisTemplate.delete(RedisConstants.OFFLINE_MSG_COUNT_PREFIX + userId);
    }
}
