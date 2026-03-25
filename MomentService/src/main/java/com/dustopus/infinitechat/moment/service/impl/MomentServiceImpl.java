package com.dustopus.infinitechat.moment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.moment.MomentDTO;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.moment.mapper.MomentCommentMapper;
import com.dustopus.infinitechat.moment.mapper.MomentLikeMapper;
import com.dustopus.infinitechat.moment.mapper.MomentMapper;
import com.dustopus.infinitechat.moment.model.Moment;
import com.dustopus.infinitechat.moment.model.MomentComment;
import com.dustopus.infinitechat.moment.model.MomentLike;
import com.dustopus.infinitechat.moment.service.MomentService;
import com.dustopus.infinitechat.moment.vo.CommentVO;
import com.dustopus.infinitechat.moment.vo.PublishMomentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long publishMoment(Long userId, PublishMomentVO vo) {
        if (vo.getContent() == null && (vo.getImages() == null || vo.getImages().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "内容和图片不能同时为空");
        }

        Moment moment = new Moment();
        moment.setMomentId(snowflakeIdGenerator.nextId());
        moment.setUserId(userId);
        moment.setContent(vo.getContent());
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        moment.setStatus(1);

        if (vo.getImages() != null && !vo.getImages().isEmpty()) {
            try {
                moment.setImages(objectMapper.writeValueAsString(vo.getImages()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize images", e);
            }
        }

        momentMapper.insert(moment);

        // Add to user's timeline cache
        stringRedisTemplate.opsForList().leftPush(
                RedisConstants.MOMENT_TIMELINE_PREFIX + userId,
                String.valueOf(moment.getMomentId()));

        return moment.getMomentId();
    }

    @Override
    public List<MomentDTO> getTimeline(Long userId, Long lastMomentId, int pageSize) {
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<Moment>()
                .eq(Moment::getStatus, 1);

        if (lastMomentId != null) {
            wrapper.lt(Moment::getMomentId, lastMomentId);
        }

        wrapper.orderByDesc(Moment::getCreatedAt);
        wrapper.last("LIMIT " + pageSize);

        List<Moment> moments = momentMapper.selectList(wrapper);
        List<MomentDTO> result = new ArrayList<>();

        // Collect unique userIds to batch-fetch user info from Redis
        Set<Long> userIds = new HashSet<>();
        for (Moment m : moments) {
            userIds.add(m.getUserId());
        }

        // Batch fetch user info from Redis
        Map<Long, String[]> userInfoMap = new HashMap<>();
        for (Long uid : userIds) {
            String userKey = "user:info:" + uid;
            Map<Object, Object> userInfo = stringRedisTemplate.opsForHash().entries(userKey);
            if (!userInfo.isEmpty()) {
                userInfoMap.put(uid, new String[]{
                        userInfo.getOrDefault("userName", "").toString(),
                        userInfo.getOrDefault("avatar", "").toString()
                });
            }
        }

        for (Moment m : moments) {
            MomentDTO dto = toDTO(m, userId);
            String[] info = userInfoMap.get(m.getUserId());
            if (info != null) {
                dto.setUserName(info[0]);
                dto.setUserAvatar(info[1]);
            }
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public void likeMoment(Long userId, Long momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || moment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // Check if already liked
        MomentLike existing = momentLikeMapper.selectOne(
                new LambdaQueryWrapper<MomentLike>()
                        .eq(MomentLike::getMomentId, momentId)
                        .eq(MomentLike::getUserId, userId));
        if (existing != null) {
            return;
        }

        MomentLike like = new MomentLike();
        like.setMomentId(momentId);
        like.setUserId(userId);
        momentLikeMapper.insert(like);

        // Increment like count
        moment.setLikeCount(moment.getLikeCount() + 1);
        momentMapper.updateById(moment);

        // Cache in Redis
        stringRedisTemplate.opsForSet().add(
                RedisConstants.MOMENT_LIKE_PREFIX + momentId,
                String.valueOf(userId));
    }

    @Override
    @Transactional
    public void unlikeMoment(Long userId, Long momentId) {
        MomentLike like = momentLikeMapper.selectOne(
                new LambdaQueryWrapper<MomentLike>()
                        .eq(MomentLike::getMomentId, momentId)
                        .eq(MomentLike::getUserId, userId));
        if (like == null) {
            return;
        }

        momentLikeMapper.deleteById(like);

        Moment moment = momentMapper.selectById(momentId);
        if (moment != null) {
            moment.setLikeCount(Math.max(0, moment.getLikeCount() - 1));
            momentMapper.updateById(moment);
        }

        stringRedisTemplate.opsForSet().remove(
                RedisConstants.MOMENT_LIKE_PREFIX + momentId,
                String.valueOf(userId));
    }

    @Override
    @Transactional
    public void commentMoment(Long userId, CommentVO vo) {
        Moment moment = momentMapper.selectById(vo.getMomentId());
        if (moment == null || moment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        MomentComment comment = new MomentComment();
        comment.setMomentId(vo.getMomentId());
        comment.setUserId(userId);
        comment.setContent(vo.getContent());
        comment.setReplyToId(vo.getReplyToId());
        momentCommentMapper.insert(comment);

        // Increment comment count
        moment.setCommentCount(moment.getCommentCount() + 1);
        momentMapper.updateById(moment);
    }

    @Override
    public List<?> getComments(Long momentId) {
        return momentCommentMapper.selectList(
                new LambdaQueryWrapper<MomentComment>()
                        .eq(MomentComment::getMomentId, momentId)
                        .orderByAsc(MomentComment::getCreatedAt));
    }

    @Override
    @Transactional
    public void deleteMoment(Long userId, Long momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || !moment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只能删除自己发布的朋友圈");
        }

        moment.setStatus(2);
        momentMapper.updateById(moment);

        stringRedisTemplate.delete(RedisConstants.MOMENT_TIMELINE_PREFIX + userId);
    }

    private MomentDTO toDTO(Moment moment, Long currentUserId) {
        MomentDTO dto = new MomentDTO();
        dto.setMomentId(moment.getMomentId());
        dto.setUserId(moment.getUserId());
        dto.setContent(moment.getContent());
        dto.setLikeCount(moment.getLikeCount());
        dto.setCommentCount(moment.getCommentCount());
        dto.setCreatedAt(moment.getCreatedAt());

        // Parse images
        if (moment.getImages() != null) {
            try {
                dto.setImages(objectMapper.readValue(moment.getImages(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            } catch (Exception e) {
                log.warn("Failed to parse images", e);
            }
        }

        // Check if current user liked
        Boolean liked = stringRedisTemplate.opsForSet().isMember(
                RedisConstants.MOMENT_LIKE_PREFIX + moment.getMomentId(),
                String.valueOf(currentUserId));
        dto.setLiked(Boolean.TRUE.equals(liked));

        return dto;
    }
}
