package com.document.notification.system.generator.service.domain.dto;

import com.document.notification.system.domain.valueobject.GenerationDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */

@Getter
@Builder
@AllArgsConstructor
public class GenerationRequest {
    private String id;
    private String sagaId;
    private String documentId;
    private String customerId;

    private Instant createdAt;
    private GenerationDocumentStatus generationDocumentStatus;
}
