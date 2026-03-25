package com.dustopus.infinitechat.offline.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.offline.service.OfflineMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/offline")
@RequiredArgsConstructor
public class OfflineController {

    private final OfflineMessageService offlineMessageService;

    /** 获取离线消息数量 */
    @GetMapping("/count")
    public Result<Long> getOfflineCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(offlineMessageService.getOfflineCount(userId));
    }

    /** 拉取离线消息 */
    @GetMapping("/pull")
    public Result<?> pullOfflineMessages(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(offlineMessageService.pullOfflineMessages(userId));
    }

    /** 标记已读 */
    @PostMapping("/read")
    public Result<?> markAsRead(@RequestHeader("X-User-Id") Long userId) {
        offlineMessageService.markAsRead(userId);
        return Result.ok();
    }
}
