package com.document.notification.system.generator.service.domain.dto;

import com.document.notification.system.domain.valueobject.GenerationDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for document generation containing all necessary information
 * to generate the document in the specified format
 *
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


    private String documentType;
    private String fileName;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Double totalAmount;
    private String deliveryAddress;
    private String documentStatus;
    private Integer itemCount;
    private Map<String, String> metadata;
}


