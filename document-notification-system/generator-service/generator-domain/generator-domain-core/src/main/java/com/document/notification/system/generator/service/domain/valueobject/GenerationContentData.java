package com.document.notification.system.generator.service.domain.valueobject;

import lombok.Builder;
import lombok.Getter;

/**
 * Typed data required to generate document content.
 * Replaces unstructured maps with an explicit domain model.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 07/03/2026
 */
@Getter
@Builder(toBuilder = true)
public class GenerationContentData {
    private final String generationId;
    private final String documentId;
    private final String customerId;
    private final String requestId;
    private final String sagaId;
}

