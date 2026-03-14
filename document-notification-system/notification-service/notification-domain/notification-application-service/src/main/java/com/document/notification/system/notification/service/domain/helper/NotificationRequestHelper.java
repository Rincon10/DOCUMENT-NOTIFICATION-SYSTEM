package com.document.notification.system.notification.service.domain.helper;

import com.document.notification.system.notification.service.domain.dto.NotificationRequest;

public interface NotificationRequestHelper {
    void persistNotificationOnHistoryRecords(NotificationRequest notificationRequest);

    void persistCancelledNotificationOnHistoryRecords(NotificationRequest notificationRequest);
}
