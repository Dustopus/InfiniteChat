package com.dustopus.infinitechat.notify.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.notify.model.Notification;
import com.dustopus.infinitechat.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<List<Notification>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long lastId) {
        return Result.ok(notificationService.getNotifications(userId, pageSize, lastId));
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/{notificationId}/read")
    public Result<?> markAsRead(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable Long notificationId) {
        notificationService.markAsRead(userId, notificationId);
        return Result.ok();
    }

    @PostMapping("/read/all")
    public Result<?> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
