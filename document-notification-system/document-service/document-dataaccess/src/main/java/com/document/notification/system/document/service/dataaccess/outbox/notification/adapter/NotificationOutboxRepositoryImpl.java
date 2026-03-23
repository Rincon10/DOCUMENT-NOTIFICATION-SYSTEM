package com.document.notification.system.document.service.dataaccess.outbox.notification.adapter;

import com.document.notification.system.document.service.dataaccess.outbox.notification.NotificationOutboxDataAccessMapperI;
import com.document.notification.system.document.service.dataaccess.outbox.notification.entity.NotificationOutboxEntity;
import com.document.notification.system.document.service.dataaccess.outbox.notification.repository.NotificationOutboxJpaRepository;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.exception.NotificationOutboxNotFoundException;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.ports.output.repository.NotificationOutboxRepository;
import com.document.notification.system.saga.SagaStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Component
@AllArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {

    private final NotificationOutboxJpaRepository jpaRepository;
    private final NotificationOutboxDataAccessMapperI notificationOutboxDataAccessMapper;

    @Override
    public DocumentNotificationOutboxMessage save(DocumentNotificationOutboxMessage documentNotificationOutboxMessage) {

        NotificationOutboxEntity outboxEntity = notificationOutboxDataAccessMapper.mapDocumentNotificationOutboxMessageToNotificationOutboxEntity(documentNotificationOutboxMessage);
        NotificationOutboxEntity savedOutboxEntity = jpaRepository.save(outboxEntity);
        return notificationOutboxDataAccessMapper.mapNotificationOutboxEntityToDocumentNotificationOutboxMessage(savedOutboxEntity);
    }

    @Override
    public Optional<List<DocumentNotificationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String sagaType, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        List<NotificationOutboxEntity> notificationOutboxEntities = jpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn(sagaType, outboxStatus, Arrays.asList(sagaStatus))
                .orElseThrow(() -> new NotificationOutboxNotFoundException("Notification Outbox object was not found for saga " + sagaType));

        List<DocumentNotificationOutboxMessage> documentNotificationOutboxMessages =
                notificationOutboxEntities.stream().map(notificationOutboxDataAccessMapper::mapNotificationOutboxEntityToDocumentNotificationOutboxMessage).collect(Collectors.toList());
        return Optional.of(documentNotificationOutboxMessages);
    }

    @Override
    public Optional<DocumentNotificationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId, SagaStatus... sagaStatus) {
        Optional<NotificationOutboxEntity> notificationOutboxEntity = jpaRepository.findByTypeAndSagaIdAndSagaStatusIn(type, sagaId, Arrays.asList(sagaStatus));
        return notificationOutboxEntity.map(notificationOutboxDataAccessMapper::mapNotificationOutboxEntityToDocumentNotificationOutboxMessage);
    }

    @Override
    public void deleteByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        jpaRepository.deleteByTypeAndOutboxStatusAndSagaStatusIn(type, outboxStatus, Arrays.asList(sagaStatus));
    }
}
