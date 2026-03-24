package com.dustopus.infinitechat.contact.service;

import com.dustopus.infinitechat.common.dto.contact.ContactDTO;
import com.dustopus.infinitechat.contact.vo.FriendRequestVO;

import java.util.List;

public interface ContactService {

    /** 发送好友申请 */
    Long sendFriendRequest(Long fromId, FriendRequestVO vo);

    /** 处理好友申请（同意/拒绝） */
    void handleFriendRequest(Long userId, Long requestId, boolean accept);

    /** 获取好友列表 */
    List<ContactDTO> getFriends(Long userId);

    /** 获取收到的好友申请列表 */
    List<?> getFriendRequests(Long userId);

    /** 删除好友 */
    void deleteFriend(Long userId, Long friendId);

    /** 更新好友备注 */
    void updateRemark(Long userId, Long friendId, String remark);

    /** 搜索用户 */
    List<?> searchUsers(String keyword);
}
