package com.document.notification.system.notification.service.dataaccess.mapper;

import com.document.notification.system.notification.service.dataaccess.entity.DocumentNotificationEntity;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;

public interface IDocumentNotificationDataMapper {
    DocumentNotificationEntity documentNotificationToDocumentNotificationEntity(DocumentNotification documentNotification);
}
