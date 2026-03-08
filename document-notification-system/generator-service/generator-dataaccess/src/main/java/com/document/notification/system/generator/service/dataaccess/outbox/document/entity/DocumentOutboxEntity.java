package com.document.notification.system.generator.service.dataaccess.outbox.document.entity;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_outbox")
public class DocumentOutboxEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sagaId;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    private ZonedDateTime processedAt;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus generationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus outboxStatus;

    @Version
    private int version;
}
