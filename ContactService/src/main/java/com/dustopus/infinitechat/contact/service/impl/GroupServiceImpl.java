package com.dustopus.infinitechat.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import com.dustopus.infinitechat.contact.mapper.GroupInfoMapper;
import com.dustopus.infinitechat.contact.mapper.GroupMemberMapper;
import com.dustopus.infinitechat.contact.model.GroupInfo;
import com.dustopus.infinitechat.contact.model.GroupMember;
import com.dustopus.infinitechat.contact.service.GroupService;
import com.dustopus.infinitechat.contact.vo.AddGroupMembersRequest;
import com.dustopus.infinitechat.contact.vo.CreateGroupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional
    public Long createGroup(Long userId, CreateGroupRequest request) {
        Long groupId = snowflakeIdGenerator.nextId();

        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupName(request.getGroupName());
        groupInfo.setAvatar(request.getAvatar());
        groupInfo.setOwnerId(userId);
        groupInfo.setMemberNum(1);
        groupInfo.setStatus(1);
        groupInfo.setCreatedAt(LocalDateTime.now());
        groupInfo.setUpdatedAt(LocalDateTime.now());
        groupInfoMapper.insert(groupInfo);

        // Add creator as group owner
        GroupMember member = new GroupMember();
        member.setId(snowflakeIdGenerator.nextId());
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(2); // 群主
        member.setCreatedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        return groupId;
    }

    @Override
    @Transactional
    public void addMembers(Long userId, AddGroupMembersRequest request) {
        Long groupId = request.getGroupId();
        GroupInfo groupInfo = groupInfoMapper.selectById(groupId);
        if (groupInfo == null || groupInfo.getStatus() != 1) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }

        // Check if requester is group member
        GroupMember requester = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (requester == null) {
            throw new BusinessException(ErrorCode.NOT_GROUP_MEMBER);
        }

        int added = 0;
        for (Long uid : request.getUserIds()) {
            // Skip if already member
            GroupMember existing = groupMemberMapper.selectOne(
                    new LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, groupId)
                            .eq(GroupMember::getUserId, uid));
            if (existing != null) continue;

            GroupMember member = new GroupMember();
            member.setId(snowflakeIdGenerator.nextId());
            member.setGroupId(groupId);
            member.setUserId(uid);
            member.setRole(0);
            member.setCreatedAt(LocalDateTime.now());
            groupMemberMapper.insert(member);
            added++;
        }

        if (added > 0) {
            groupInfo.setMemberNum(groupInfo.getMemberNum() + added);
            groupInfoMapper.updateById(groupInfo);
            stringRedisTemplate.delete(RedisConstants.GROUP_MEMBER_PREFIX + groupId);
        }
    }

    @Override
    public Map<String, Object> getGroupInfo(Long groupId) {
        GroupInfo groupInfo = groupInfoMapper.selectById(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupInfo.getGroupId());
        result.put("groupName", groupInfo.getGroupName());
        result.put("avatar", groupInfo.getAvatar());
        result.put("ownerId", groupInfo.getOwnerId());
        result.put("notice", groupInfo.getNotice());
        result.put("memberNum", groupInfo.getMemberNum());
        result.put("status", groupInfo.getStatus());
        result.put("createdAt", groupInfo.getCreatedAt());
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUserId, userId));
        return memberships.stream().map(m -> {
            GroupInfo groupInfo = groupInfoMapper.selectById(m.getGroupId());
            Map<String, Object> map = new HashMap<>();
            if (groupInfo != null) {
                map.put("groupId", groupInfo.getGroupId());
                map.put("groupName", groupInfo.getGroupName());
                map.put("avatar", groupInfo.getAvatar());
                map.put("memberNum", groupInfo.getMemberNum());
                map.put("role", m.getRole());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .orderByAsc(GroupMember::getRole));
        return members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", m.getUserId());
            map.put("role", m.getRole());
            map.put("nickname", m.getNickname());
            map.put("joinedAt", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void quitGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_GROUP_MEMBER);
        }
        if (member.getRole() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "群主不能直接退出，请先转让或解散群聊");
        }

        groupMemberMapper.deleteById(member.getId());
        GroupInfo groupInfo = groupInfoMapper.selectById(groupId);
        if (groupInfo != null) {
            groupInfo.setMemberNum(groupInfo.getMemberNum() - 1);
            groupInfoMapper.updateById(groupInfo);
        }
        stringRedisTemplate.delete(RedisConstants.GROUP_MEMBER_PREFIX + groupId);
    }

    @Override
    @Transactional
    public void dissolveGroup(Long userId, Long groupId) {
        GroupInfo groupInfo = groupInfoMapper.selectById(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (!groupInfo.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只有群主可以解散群聊");
        }

        groupInfo.setStatus(2);
        groupInfo.setUpdatedAt(LocalDateTime.now());
        groupInfoMapper.updateById(groupInfo);

        groupMemberMapper.delete(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));
        stringRedisTemplate.delete(RedisConstants.GROUP_MEMBER_PREFIX + groupId);
    }
}
