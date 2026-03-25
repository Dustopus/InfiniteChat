package com.dustopus.infinitechat.messaging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.messaging.mapper.MessageMapper;
import com.dustopus.infinitechat.messaging.mapper.RedPacketMapper;
import com.dustopus.infinitechat.messaging.mapper.RedPacketRecordMapper;
import com.dustopus.infinitechat.messaging.model.Message;
import com.dustopus.infinitechat.messaging.model.RedPacket;
import com.dustopus.infinitechat.messaging.model.RedPacketRecord;
import com.dustopus.infinitechat.messaging.service.RedPacketService;
import com.dustopus.infinitechat.messaging.vo.SendRedPacketVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedPacketServiceImpl implements RedPacketService {

    private final RedPacketMapper redPacketMapper;
    private final RedPacketRecordMapper redPacketRecordMapper;
    private final MessageMapper messageMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Long sendRedPacket(Long senderId, SendRedPacketVO vo) {
        // 验证参数
        if (vo.getTotalAmount() == null || vo.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "红包金额必须大于0");
        }
        if (vo.getTotalCount() == null || vo.getTotalCount() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "红包数量必须大于0");
        }
        if (vo.getTotalAmount().compareTo(new BigDecimal("200")) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "单个红包金额不能超过200元");
        }
        if (vo.getReceiverId() == null && vo.getGroupId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "必须指定接收者或群组");
        }

        Long packetId = snowflakeIdGenerator.nextId();

        // 创建红包记录
        RedPacket redPacket = new RedPacket();
        redPacket.setPacketId(packetId);
        redPacket.setSenderId(senderId);
        redPacket.setGroupId(vo.getGroupId());
        redPacket.setTotalAmount(vo.getTotalAmount());
        redPacket.setTotalCount(vo.getTotalCount());
        redPacket.setGrabbedCount(0);
        redPacket.setGreeting(vo.getGreeting() != null ? vo.getGreeting() : "恭喜发财，大吉大利！");
        redPacket.setStatus(1);
        redPacket.setExpireAt(LocalDateTime.now().plusDays(1)); // 24小时过期
        redPacket.setCreatedAt(LocalDateTime.now());
        redPacketMapper.insert(redPacket);

        // 缓存到Redis，用于快速抢红包
        String redisKey = RedisConstants.RED_PACKET_PREFIX + packetId;
        Map<String, String> packetInfo = new HashMap<>();
        packetInfo.put("senderId", String.valueOf(senderId));
        packetInfo.put("totalAmount", vo.getTotalAmount().toPlainString());
        packetInfo.put("totalCount", String.valueOf(vo.getTotalCount()));
        packetInfo.put("grabbedCount", "0");
        stringRedisTemplate.opsForHash().putAll(redisKey, packetInfo);
        stringRedisTemplate.expire(redisKey, java.time.Duration.ofDays(1));

        // 发送红包消息
        try {
            Message message = new Message();
            message.setMessageId(snowflakeIdGenerator.nextId());
            message.setSenderId(senderId);
            message.setReceiverId(vo.getReceiverId());
            message.setGroupId(vo.getGroupId());
            message.setChatType(vo.getGroupId() != null ? MessageConstants.GROUP_CHAT : MessageConstants.SINGLE_CHAT);
            message.setMsgType(6); // 红包类型
            message.setContent(vo.getGreeting() != null ? vo.getGreeting() : "发了一个红包");
            Map<String, Object> extra = new HashMap<>();
            extra.put("packetId", packetId);
            extra.put("totalAmount", vo.getTotalAmount());
            extra.put("totalCount", vo.getTotalCount());
            message.setExtra(objectMapper.writeValueAsString(extra));
            message.setStatus(1);
            messageMapper.insert(message);

            MessageDTO dto = new MessageDTO();
            dto.setMessageId(message.getMessageId());
            dto.setSenderId(senderId);
            dto.setReceiverId(vo.getReceiverId());
            dto.setGroupId(vo.getGroupId());
            dto.setChatType(message.getChatType());
            dto.setMsgType(6);
            dto.setContent(message.getContent());
            dto.setExtra(message.getExtra());
            dto.setTimestamp(System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(MessageConstants.KAFKA_TOPIC_MESSAGE, json);

            if (vo.getGroupId() != null) {
                publishGroupMessage(vo.getGroupId(), json);
            }
        } catch (Exception e) {
            log.error("Failed to send red packet message", e);
        }

        return packetId;
    }

    @Override
    @Transactional
    public BigDecimal grabRedPacket(Long userId, Long packetId) {
        // Use Redisson distributed lock to prevent race conditions
        String lockKey = "redpacket:lock:" + packetId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统繁忙，请稍后再试");
            }
            return doGrabRedPacket(userId, packetId);
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("Failed to grab red packet", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private BigDecimal doGrabRedPacket(Long userId, Long packetId) {
        // 从Redis获取红包信息
        String redisKey = RedisConstants.RED_PACKET_PREFIX + packetId;
        Map<Object, Object> packetInfo = stringRedisTemplate.opsForHash().entries(redisKey);

        if (packetInfo.isEmpty()) {
            RedPacket redPacket = redPacketMapper.selectById(packetId);
            if (redPacket == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            if (redPacket.getStatus() == 2) {
                throw new BusinessException(ErrorCode.RED_PACKET_EMPTY);
            }
            if (redPacket.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.RED_PACKET_EXPIRED);
            }
            // 回填Redis
            packetInfo.put("senderId", String.valueOf(redPacket.getSenderId()));
            packetInfo.put("totalAmount", redPacket.getTotalAmount().toPlainString());
            packetInfo.put("totalCount", String.valueOf(redPacket.getTotalCount()));
            packetInfo.put("grabbedCount", String.valueOf(redPacket.getGrabbedCount()));
            stringRedisTemplate.opsForHash().putAll(redisKey, packetInfo);
            stringRedisTemplate.expire(redisKey, java.time.Duration.ofDays(1));
        }

        Long senderId = Long.parseLong(packetInfo.get("senderId").toString());
        if (senderId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能抢自己发的红包");
        }

        // 检查是否已经抢过
        String recordKey = RedisConstants.RED_PACKET_RECORD_PREFIX + packetId;
        Boolean alreadyGrabbed = stringRedisTemplate.opsForSet().isMember(recordKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(alreadyGrabbed)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "你已经抢过这个红包了");
        }

        int totalCount = Integer.parseInt(packetInfo.get("totalCount").toString());
        int grabbedCount = Integer.parseInt(packetInfo.get("grabbedCount").toString());

        if (grabbedCount >= totalCount) {
            throw new BusinessException(ErrorCode.RED_PACKET_EMPTY);
        }

        // 计算红包金额 - 随机分配
        BigDecimal totalAmount = new BigDecimal(packetInfo.get("totalAmount").toString());
        BigDecimal amount;

        int remaining = totalCount - grabbedCount;
        if (remaining == 1) {
            // 最后一个红包，拿走剩余金额
            amount = totalAmount.subtract(getGrabbedTotal(packetId));
        } else {
            // 随机金额，至少0.01元，最多不超过剩余平均值的2倍
            BigDecimal grabbedTotal = getGrabbedTotal(packetId);
            BigDecimal remainingAmount = totalAmount.subtract(grabbedTotal);
            BigDecimal avg = remainingAmount.divide(BigDecimal.valueOf(remaining), 2, RoundingMode.HALF_UP);
            BigDecimal maxAmount = avg.multiply(BigDecimal.valueOf(2)).min(remainingAmount.subtract(new BigDecimal("0.01").multiply(BigDecimal.valueOf(remaining - 1))));
            if (maxAmount.compareTo(new BigDecimal("0.01")) < 0) {
                maxAmount = new BigDecimal("0.01");
            }

            Random random = new Random();
            double ratio = 0.01 + random.nextDouble() * 0.98; // 1% 到 99%
            amount = maxAmount.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(new BigDecimal("0.01")) < 0) {
                amount = new BigDecimal("0.01");
            }
        }

        // 创建领取记录
        RedPacketRecord record = new RedPacketRecord();
        record.setPacketId(packetId);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setCreatedAt(LocalDateTime.now());
        redPacketRecordMapper.insert(record);

        // 更新Redis
        stringRedisTemplate.opsForHash().increment(redisKey, "grabbedCount", 1);
        stringRedisTemplate.opsForSet().add(recordKey, String.valueOf(userId));

        // 更新数据库
        grabbedCount++;
        RedPacket redPacket = redPacketMapper.selectById(packetId);
        redPacket.setGrabbedCount(grabbedCount);
        if (grabbedCount >= totalCount) {
            redPacket.setStatus(2); // 已领完
        }
        redPacketMapper.updateById(redPacket);

        return amount;
    }

    @Override
    public Map<String, Object> getRedPacketDetail(Long userId, Long packetId) {
        RedPacket redPacket = redPacketMapper.selectById(packetId);
        if (redPacket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("packetId", redPacket.getPacketId());
        detail.put("senderId", redPacket.getSenderId());
        detail.put("groupId", redPacket.getGroupId());
        detail.put("totalAmount", redPacket.getTotalAmount());
        detail.put("totalCount", redPacket.getTotalCount());
        detail.put("grabbedCount", redPacket.getGrabbedCount());
        detail.put("greeting", redPacket.getGreeting());
        detail.put("status", redPacket.getStatus());
        detail.put("expireAt", redPacket.getExpireAt());
        detail.put("createdAt", redPacket.getCreatedAt());

        // 检查当前用户是否已领取
        String recordKey = RedisConstants.RED_PACKET_RECORD_PREFIX + packetId;
        Boolean alreadyGrabbed = stringRedisTemplate.opsForSet().isMember(recordKey, String.valueOf(userId));
        detail.put("alreadyGrabbed", Boolean.TRUE.equals(alreadyGrabbed));

        if (Boolean.TRUE.equals(alreadyGrabbed)) {
            // 获取用户领取的金额
            RedPacketRecord record = redPacketRecordMapper.selectOne(
                    new LambdaQueryWrapper<RedPacketRecord>()
                            .eq(RedPacketRecord::getPacketId, packetId)
                            .eq(RedPacketRecord::getUserId, userId));
            if (record != null) {
                detail.put("grabbedAmount", record.getAmount());
            }
        }

        return detail;
    }

    @Override
    public List<Map<String, Object>> getMyRedPackets(Long userId) {
        // 获取用户发送的红包
        List<RedPacket> sentPackets = redPacketMapper.selectList(
                new LambdaQueryWrapper<RedPacket>()
                        .eq(RedPacket::getSenderId, userId)
                        .orderByDesc(RedPacket::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (RedPacket packet : sentPackets) {
            Map<String, Object> map = new HashMap<>();
            map.put("packetId", packet.getPacketId());
            map.put("totalAmount", packet.getTotalAmount());
            map.put("totalCount", packet.getTotalCount());
            map.put("grabbedCount", packet.getGrabbedCount());
            map.put("status", packet.getStatus());
            map.put("createdAt", packet.getCreatedAt());
            map.put("type", "sent");
            result.add(map);
        }

        // 获取用户领取的红包
        List<RedPacketRecord> receivedRecords = redPacketRecordMapper.selectList(
                new LambdaQueryWrapper<RedPacketRecord>()
                        .eq(RedPacketRecord::getUserId, userId)
                        .orderByDesc(RedPacketRecord::getCreatedAt));

        for (RedPacketRecord record : receivedRecords) {
            RedPacket packet = redPacketMapper.selectById(record.getPacketId());
            if (packet != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("packetId", packet.getPacketId());
                map.put("senderId", packet.getSenderId());
                map.put("grabbedAmount", record.getAmount());
                map.put("createdAt", record.getCreatedAt());
                map.put("type", "received");
                result.add(map);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getRedPacketRecords(Long packetId) {
        List<RedPacketRecord> records = redPacketRecordMapper.selectList(
                new LambdaQueryWrapper<RedPacketRecord>()
                        .eq(RedPacketRecord::getPacketId, packetId)
                        .orderByAsc(RedPacketRecord::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (RedPacketRecord record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", record.getUserId());
            map.put("amount", record.getAmount());
            map.put("grabbedAt", record.getCreatedAt());
            result.add(map);
        }

        return result;
    }

    private BigDecimal getGrabbedTotal(Long packetId) {
        List<RedPacketRecord> records = redPacketRecordMapper.selectList(
                new LambdaQueryWrapper<RedPacketRecord>()
                        .eq(RedPacketRecord::getPacketId, packetId));

        BigDecimal total = BigDecimal.ZERO;
        for (RedPacketRecord record : records) {
            total = total.add(record.getAmount());
        }
        return total;
    }

    private void publishGroupMessage(Long groupId, String messageJson) {
        try {
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
}
