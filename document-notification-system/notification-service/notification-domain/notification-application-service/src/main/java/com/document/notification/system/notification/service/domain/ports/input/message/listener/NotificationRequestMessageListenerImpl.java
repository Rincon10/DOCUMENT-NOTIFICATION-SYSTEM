package com.document.notification.system.notification.service.domain.ports.input.message.listener;

import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.helper.NotificationRequestHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationRequestMessageListenerImpl implements NotificationRequestMessageListener {

    private final NotificationRequestHelper notificationRequestHelper;

    @Override
    public void processNotification(NotificationRequest notificationRequest) {
        notificationRequestHelper.persistNotificationOnHistoryRecords(notificationRequest);
    }

    @Override
    public void cancelNotification(NotificationRequest notificationRequest) {
        notificationRequestHelper.persistCancelledNotificationOnHistoryRecords(notificationRequest);
    }
}
