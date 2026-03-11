package com.document.notification.system.outbox.model.notification;

import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.saga.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class DocumentNotificationOutboxMessage {
    private UUID id;
    private UUID sagaId;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;
    private String payload;
    private SagaStatus sagaStatus;
    private DocumentStatus documentStatus;
    private OutboxStatus outboxStatus;
    private int version;

}
