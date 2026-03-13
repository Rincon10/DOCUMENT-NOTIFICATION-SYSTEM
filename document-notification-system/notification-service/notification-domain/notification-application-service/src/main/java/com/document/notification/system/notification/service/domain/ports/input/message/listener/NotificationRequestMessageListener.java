package com.document.notification.system.notification.service.domain.ports.input.message.listener;

import com.document.notification.system.notification.service.domain.dto.NotificationRequest;

public interface NotificationRequestMessageListener {
    void processNotification(NotificationRequest notificationRequest);

    void cancelNotification(NotificationRequest notificationRequest);
}
