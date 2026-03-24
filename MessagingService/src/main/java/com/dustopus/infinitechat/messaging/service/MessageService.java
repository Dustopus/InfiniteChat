package com.dustopus.infinitechat.messaging.service;

import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.messaging.vo.SendMessageVO;

import java.util.List;

public interface MessageService {

    /** 发送消息（单聊/群聊） */
    MessageDTO sendMessage(Long senderId, SendMessageVO vo);

    /** 获取聊天历史记录 */
    List<MessageDTO> getChatHistory(Long userId, Long targetId, String chatType,
                                     Long lastMessageId, int pageSize);

    /** 撤回消息 */
    void recallMessage(Long userId, Long messageId);

    /** 获取最近会话列表 */
    List<?> getRecentChats(Long userId);
}
