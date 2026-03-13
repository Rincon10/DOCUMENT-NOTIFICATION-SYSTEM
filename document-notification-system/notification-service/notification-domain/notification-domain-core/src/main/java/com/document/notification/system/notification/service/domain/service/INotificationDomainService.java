package com.document.notification.system.notification.service.domain.service;

import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;

import java.util.List;

public interface INotificationDomainService {
    NotificationEvent validateAndSendNotification(DocumentNotification documentNotification,
                                                   List<String> failureMessages);
}
