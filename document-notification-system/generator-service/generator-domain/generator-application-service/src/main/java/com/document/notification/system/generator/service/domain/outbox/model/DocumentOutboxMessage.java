package com.document.notification.system.generator.service.domain.outbox.model;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentOutboxMessage {
    private UUID id;
    private UUID sagaId;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;
    private String payload;
    private GenerationStatus generationStatus;

    @Setter
    private OutboxStatus outboxStatus;
    private int version;

}
