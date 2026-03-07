package com.document.notification.system.generator.service.domain.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.valueobject.GenerationId;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
@Getter
public class DocumentGeneration extends AggregateRoot<GenerationId> {
    protected static final Map<String, DocumentType> VALID_FILE_EXTENSIONS = new HashMap<>();

    static {
        for (DocumentType documentType : DocumentType.values()) {
            VALID_FILE_EXTENSIONS.put(documentType.name(), documentType);
        }
    }

    private final GenerationId generationId;
    private final DocumentId documentId;
    private final CustomerId customerId;
    private final DocumentType fileExtension;
    private final List<String> failureMessages;
    private final DocumentType documentType;
    private GenerationStatus generationStatus;
    private LocalDateTime createdAt;
    private String generatedContentBase64;

    @Builder
    public DocumentGeneration(GenerationId generationId,
                              DocumentId documentId,
                              CustomerId customerId,
                              GenerationStatus generationStatus,
                              LocalDateTime createdAt,
                              DocumentType fileExtension,
                              List<String> failureMessages,
                              DocumentType documentType,
                              String generatedContentBase64) {
        this.generationId = generationId;
        this.documentId = documentId;
        this.customerId = customerId;
        this.generationStatus = generationStatus;
        this.createdAt = createdAt;
        this.fileExtension = fileExtension;
        this.failureMessages = failureMessages;
        this.documentType = documentType;
        this.generatedContentBase64 = generatedContentBase64;
    }

    public void initializateGeneration() {
        setId(new GenerationId(UUID.randomUUID()));
        this.createdAt = DateUtils.getZoneDateTimeByUTCZoneId().toLocalDateTime();
    }

    public void validateGeneration(List<String> validationErrors, String fileExtension) {
        if (Objects.isNull(fileExtension) || !VALID_FILE_EXTENSIONS.containsKey(fileExtension.toUpperCase())) {
            validationErrors.add("Invalid file extension: " + fileExtension);
        }


    }

    public void updateStatus(GenerationStatus generationStatus) {
        this.generationStatus = generationStatus;
    }

    public void setGeneratedContent(String base64Content) {
        this.generatedContentBase64 = base64Content;
    }

}
