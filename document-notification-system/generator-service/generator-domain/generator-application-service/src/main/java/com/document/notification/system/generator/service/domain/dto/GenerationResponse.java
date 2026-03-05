package com.document.notification.system.generator.service.domain.dto;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for document generation
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class GenerationResponse {
    private String generationId;
    private String sagaId;
    private String documentId;
    private String customerId;
    private ZonedDateTime createdAt;
    private GenerationStatus generationStatus;
    private List<String> failureMessages;
}

