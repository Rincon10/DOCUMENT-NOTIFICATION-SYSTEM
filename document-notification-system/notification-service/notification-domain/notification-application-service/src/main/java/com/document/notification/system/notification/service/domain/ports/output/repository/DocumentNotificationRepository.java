package com.document.notification.system.notification.service.domain.ports.output.repository;

import com.document.notification.system.notification.service.domain.entity.DocumentNotification;

import java.util.Optional;
import java.util.UUID;

public interface DocumentNotificationRepository {
    DocumentNotification save(DocumentNotification documentNotification);

    Optional<DocumentNotification> findByDocumentId(UUID documentId);
}
