package com.document.notification.system.document.service.messaging.mapper;

import com.document.notification.system.dto.message.CustomerModel;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.kafka.document.avro.model.CustomerAvroModel;
import com.document.notification.system.kafka.document.avro.model.DocumentGenerationStatus;
import com.document.notification.system.kafka.document.avro.model.DocumentStatus;
import com.document.notification.system.kafka.document.avro.model.DocumentType;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @Override
    public GenerationResponse generatorResponseAvroModelToGenerationResponse(GeneratorResponseAvroModel generatorResponseAvroModel) {
        throw new UnsupportedOperationException("Method not implemented yet");
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

    private String buildFileName(DocumentGenerationEventPayload payload) {
        String documentType = StringUtils.trimToEmpty(payload.getDocumentType()).toLowerCase(Locale.ROOT);
        return "document-" + payload.getDocumentId() + (documentType.isEmpty() ? "" : "." + documentType);
    }
}
