package com.document.notification.system.notification.service.dataaccess.mapper;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.notification.service.dataaccess.entity.DocumentNotificationEntity;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class DocumentNotificationDataMapper implements IDocumentNotificationDataMapper {
    @Override
    public DocumentNotificationEntity documentNotificationToDocumentNotificationEntity(DocumentNotification documentNotification) {
        return DocumentNotificationEntity.builder()
                .id(documentNotification.getId().getValue())
                .customerId(documentNotification.getCustomerId().getValue())
                .documentId(documentNotification.getDocumentId().getValue())
                .recipientId(documentNotification.getRecipient() != null
                        ? documentNotification.getRecipient().getTarget()
                        : null)
                .subject(documentNotification.getNotificationContent() != null
                        ? documentNotification.getNotificationContent().getSubject()
                        : null)
                .status(documentNotification.getNotificationStatus())
                .createdAt(DateUtils.getZoneDateTimeByUTCZoneId())
                .build();
    }
}
