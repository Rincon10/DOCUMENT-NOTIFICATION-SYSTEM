package com.document.notification.system.notification.service.dataaccess.adapter;

import com.document.notification.system.notification.service.dataaccess.entity.DocumentNotificationEntity;
import com.document.notification.system.notification.service.dataaccess.mapper.IDocumentNotificationDataMapper;
import com.document.notification.system.notification.service.dataaccess.repository.DocumentNotificationJpaRepository;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentNotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class DocumentNotificationRepositoryImpl implements DocumentNotificationRepository {

    private final DocumentNotificationJpaRepository documentNotificationJpaRepository;
    private final IDocumentNotificationDataMapper documentNotificationDataMapper;

    @Override
    public DocumentNotification save(DocumentNotification documentNotification) {
        DocumentNotificationEntity entity = documentNotificationDataMapper
                .documentNotificationToDocumentNotificationEntity(documentNotification);
        documentNotificationJpaRepository.save(entity);
        return documentNotification;
    }

    @Override
    public Optional<DocumentNotification> findByDocumentId(UUID documentId) {
        return documentNotificationJpaRepository.findByDocumentId(documentId)
                .map(documentNotificationDataMapper::documentNotificationEntityToDocumentNotification);
    }
}
