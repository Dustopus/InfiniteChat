package com.dustopus.infinitechat.notify.service;

import com.dustopus.infinitechat.notify.model.Notification;
import java.util.List;

public interface NotificationService {
    /** 获取通知列表 */
    List<Notification> getNotifications(Long userId, int pageSize, Long lastId);
    /** 获取未读通知数量 */
    Long getUnreadCount(Long userId);
    /** 标记已读 */
    void markAsRead(Long userId, Long notificationId);
    /** 全部标记已读 */
    void markAllAsRead(Long userId);
    /** 创建通知 */
    void createNotification(Long userId, String type, String content, Long refId);
}
