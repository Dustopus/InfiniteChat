package com.dustopus.infinitechat.contact.service;

import com.dustopus.infinitechat.contact.vo.AddGroupMembersRequest;
import com.dustopus.infinitechat.contact.vo.CreateGroupRequest;

import java.util.List;
import java.util.Map;

public interface GroupService {
    /** 创建群聊 */
    Long createGroup(Long userId, CreateGroupRequest request);
    /** 添加群成员 */
    void addMembers(Long userId, AddGroupMembersRequest request);
    /** 获取群信息 */
    Map<String, Object> getGroupInfo(Long groupId);
    /** 获取用户所在群列表 */
    List<Map<String, Object>> getUserGroups(Long userId);
    /** 获取群成员列表 */
    List<Map<String, Object>> getGroupMembers(Long groupId);
    /** 退出群聊 */
    void quitGroup(Long userId, Long groupId);
    /** 解散群聊 */
    void dissolveGroup(Long userId, Long groupId);
}
