package com.document.notification.system.document.service.dataaccess.outbox.notification.entity;

import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.saga.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_outbox")
@Entity
public class NotificationOutboxEntity {
    @Id
    private UUID id;
    private UUID sagaId;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;
    private String payload;

    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;
    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;
    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;
    @Version
    private int version;

}
