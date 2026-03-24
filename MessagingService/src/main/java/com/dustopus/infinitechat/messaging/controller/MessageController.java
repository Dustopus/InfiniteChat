package com.dustopus.infinitechat.messaging.controller;

import com.dustopus.infinitechat.common.dto.message.MessageDTO;
import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.messaging.service.MessageService;
import com.dustopus.infinitechat.messaging.vo.SendMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 发送消息 */
    @PostMapping("/send")
    public Result<MessageDTO> sendMessage(@RequestHeader("X-User-Id") Long userId,
                                           @RequestBody @Valid SendMessageVO vo) {
        return Result.ok(messageService.sendMessage(userId, vo));
    }

    /** 获取聊天历史 */
    @GetMapping("/history")
    public Result<List<MessageDTO>> getHistory(@RequestHeader("X-User-Id") Long userId,
                                                @RequestParam String chatType,
                                                @RequestParam Long targetId,
                                                @RequestParam(required = false) Long lastMessageId,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(messageService.getChatHistory(userId, targetId, chatType, lastMessageId, pageSize));
    }

    /** 撤回消息 */
    @PostMapping("/{messageId}/recall")
    public Result<?> recallMessage(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long messageId) {
        messageService.recallMessage(userId, messageId);
        return Result.ok();
    }

    /** 获取最近会话 */
    @GetMapping("/recent")
    public Result<?> getRecentChats(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(messageService.getRecentChats(userId));
    }
}
