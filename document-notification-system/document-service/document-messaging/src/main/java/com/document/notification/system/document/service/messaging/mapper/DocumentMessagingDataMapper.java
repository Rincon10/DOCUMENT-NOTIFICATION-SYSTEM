package com.document.notification.system.document.service.messaging.mapper;

import com.document.notification.system.dto.message.CustomerModel;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.dto.message.NotificationResponse;
import com.document.notification.system.domain.valueobject.DocumentNotificationStatus;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.kafka.document.avro.model.*;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Component
public class DocumentMessagingDataMapper implements IDocumentMessagingDataMapper {
    @Override
    public CustomerModel customerAvroModeltoCustomerModel(CustomerAvroModel customerAvroModel) {
        return CustomerModel.builder()
                .id(customerAvroModel.getId())
                .username(customerAvroModel.getUsername())
                .firstName(customerAvroModel.getFirstName())
                .lastName(customerAvroModel.getLastName())
                .build();
    }

    @Override
    public GenerationResponse generatorResponseAvroModelToGenerationResponse(GeneratorResponseAvroModel generatorResponseAvroModel) {
        return GenerationResponse.builder()
                .id(UUID.fromString(generatorResponseAvroModel.getId()))
                .sagaId(UUID.fromString(generatorResponseAvroModel.getSagaId()))
                .generatorId(UUID.fromString(generatorResponseAvroModel.getGeneratorId()))
                .customerId(UUID.fromString(generatorResponseAvroModel.getCustomerId()))
                .documentId(UUID.fromString(generatorResponseAvroModel.getDocumentId()))
                .createdAt(generatorResponseAvroModel.getCreatedAt())
                .generationStatus(mapGenerationStatus(generatorResponseAvroModel.getGenerationStatus()))
                .failureMessages(safeList(generatorResponseAvroModel.getFailureMessages()))
                .fileName(generatorResponseAvroModel.getFileName())
                .contentType(generatorResponseAvroModel.getContentType())
                .contentBase64(generatorResponseAvroModel.getContentBase64())
                .fileSizeInBytes(generatorResponseAvroModel.getFileSizeInBytes())
                .build();
    }

    @Override
    public NotificationResponse notificationResponseAvroModelToNotificationResponse(NotificationResponseAvroModel notificationResponseAvroModel) {
        return NotificationResponse.builder()
                .id(notificationResponseAvroModel.getId())
                .sagaId(notificationResponseAvroModel.getSagaId())
                .documentId(notificationResponseAvroModel.getDocumentId())
                .recipentId(notificationResponseAvroModel.getRecipientId())
                .createdAt(notificationResponseAvroModel.getCreatedAt())
                .documentNotificationStatus(DocumentNotificationStatus.valueOf(notificationResponseAvroModel.getNotificationStatus().name()))
                .failureMessages(safeList(notificationResponseAvroModel.getFailureMessages()))
                .build();
    }

    @Override
    public GeneratorRequestAvroModel documentGenerationEventPayloadToGeneratorRequestAvroModel(String sagaId, DocumentGenerationEventPayload documentGenerationEventPayload) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "document-service");
        metadata.put("documentId", documentGenerationEventPayload.getDocumentId());
        metadata.put("customerId", documentGenerationEventPayload.getCustomerId());

        return GeneratorRequestAvroModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setSagaId(sagaId)
                .setCustomerId(documentGenerationEventPayload.getCustomerId())
                .setDocumentId(documentGenerationEventPayload.getDocumentId())
                .setCreatedAt(documentGenerationEventPayload.getCreatedAt().toInstant())
                .setDocumentGenerationStatus(
                        mapDocumentGenerationStatus(documentGenerationEventPayload.getDocumentGenerationStatus())
                )
                .setDocumentType(mapDocumentType(documentGenerationEventPayload.getDocumentType()))
                .setFileName(buildFileName(documentGenerationEventPayload))
                .setPeriodStartDate(null)
                .setPeriodEndDate(null)
                .setTotalAmount(null)
                .setDeliveryAddress(null)
                .setDocumentStatus(mapDocumentStatus(documentGenerationEventPayload.getDocumentGenerationStatus()))
                .setItemCount(null)
                .setMetadata(metadata)
                .build();
    }

    @Override
    public NotificationRequestAvroModel documentNotificationEventPayloadToNotificationRequestAvroModel(String sagaId, DocumentNotificationEventPayload documentNotificationEventPayload) {
        return NotificationRequestAvroModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setSagaId(sagaId)
                .setCustomerId(documentNotificationEventPayload.getCustomerId())
                .setDocumentId(documentNotificationEventPayload.getDocumentId())
                .setCreatedAt(documentNotificationEventPayload.getCreatedAt().toInstant())
                .setDocumentNotificationStatus(com.document.notification.system.kafka.document.avro.model.DocumentNotificationStatus.valueOf(documentNotificationEventPayload.getDocumentNotificationStatus()))
                .setRecipientId(resolveRecipientId(documentNotificationEventPayload))
                .setRecipientEmail(documentNotificationEventPayload.getRecipientEmail())
                .setSubject(buildNotificationSubject(documentNotificationEventPayload))
                .setMessage(buildNotificationMessage(documentNotificationEventPayload))
                .setFileName(documentNotificationEventPayload.getFileName())
                .setContentType(StringUtils.defaultIfBlank(documentNotificationEventPayload.getContentType(), "application/octet-stream"))
                .setContentBase64(documentNotificationEventPayload.getContentBase64())
                .setFailureMessages(safeList(documentNotificationEventPayload.getFailureMessages()))
                .build();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String resolveRecipientId(DocumentNotificationEventPayload payload) {
        return StringUtils.defaultIfBlank(payload.getRecipientId(), payload.getCustomerId());
    }

    private String buildNotificationSubject(DocumentNotificationEventPayload payload) {
        return StringUtils.defaultIfBlank(payload.getSubject(), "Document generated - " + payload.getDocumentId());
    }

    private String buildNotificationMessage(DocumentNotificationEventPayload payload) {
        return StringUtils.defaultIfBlank(payload.getMessage(),
                "Your document " + payload.getDocumentId() + " has been generated successfully.");
    }

    private DocumentGenerationStatus mapDocumentGenerationStatus(String status) {
        String normalizedStatus = StringUtils.trimToEmpty(status).toUpperCase(Locale.ROOT);
        return switch (normalizedStatus) {
            case "CANCELLED", "GENERATION_CANCELLED" -> DocumentGenerationStatus.CANCELLED;
            default -> DocumentGenerationStatus.PENDING;
        };
    }

    private DocumentType mapDocumentType(String documentType) {
        return DocumentType.valueOf(StringUtils.trimToEmpty(documentType).toUpperCase(Locale.ROOT));
    }

    private DocumentStatus mapDocumentStatus(String generationStatus) {
        String normalizedStatus = StringUtils.trimToEmpty(generationStatus).toUpperCase(Locale.ROOT);
        return switch (normalizedStatus) {
            case "CANCELLED", "GENERATION_CANCELLED" -> DocumentStatus.CANCELLED;
            default -> DocumentStatus.PENDING;
        };
    }

    private GenerationStatus mapGenerationStatus(com.document.notification.system.kafka.document.avro.model.GenerationStatus generationStatus) {
        String normalizedStatus = Objects.isNull(generationStatus) ? "GENERATION_FAILED" : generationStatus.name();
        return switch (normalizedStatus) {
            case "GENERATION_COMPLETED" -> GenerationStatus.GENERATION_COMPLETED;
            case "GENERATION_CANCELLED" -> GenerationStatus.GENERATION_CANCELLED;
            default -> GenerationStatus.GENERATION_FAILED;
        };
    }

    private String buildFileName(DocumentGenerationEventPayload payload) {
        String documentType = StringUtils.trimToEmpty(payload.getDocumentType()).toLowerCase(Locale.ROOT);
        return "document-" + payload.getDocumentId() + (documentType.isEmpty() ? "" : "." + documentType);
    }

}
