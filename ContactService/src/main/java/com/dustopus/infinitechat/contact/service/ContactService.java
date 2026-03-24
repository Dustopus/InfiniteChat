package com.dustopus.infinitechat.contact.service;

import com.dustopus.infinitechat.common.dto.contact.ContactDTO;
import com.dustopus.infinitechat.common.dto.user.UserDTO;
import com.dustopus.infinitechat.contact.vo.FriendApplyRequest;
import com.dustopus.infinitechat.contact.vo.FriendHandleRequest;

import java.util.List;

public interface ContactService {

    /** 发送好友申请 */
    void applyFriend(Long userId, FriendApplyRequest request);

    /** 处理好友申请（同意/拒绝） */
    void handleFriendRequest(Long userId, FriendHandleRequest request);

    /** 获取好友列表 */
    List<ContactDTO> getContactList(Long userId);

    /** 获取收到的好友申请列表 */
    List<Object> getFriendRequests(Long userId);

    /** 删除好友 */
    void deleteFriend(Long userId, Long friendId);

    /** 更新好友备注 */
    void updateRemark(Long userId, Long friendId, String remark);

    /** 搜索用户 */
    List<UserDTO> searchUsers(String keyword);
}
