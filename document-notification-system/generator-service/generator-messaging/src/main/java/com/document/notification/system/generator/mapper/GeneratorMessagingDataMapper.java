package com.document.notification.system.generator.mapper;

import com.document.notification.system.domain.valueobject.GenerationDocumentStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper for converting between Avro models and domain DTOs
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
public class GeneratorMessagingDataMapper {

    /**
     * Converts GeneratorRequestAvroModel to GenerationRequest DTO
     * Maps all fields including optional metadata for PDF generation
     */
    public GenerationRequest generatorRequestAvroModelToGenerationRequest(GeneratorRequestAvroModel generatorRequestAvroModel) {
        return GenerationRequest.builder()
                .id(generatorRequestAvroModel.getId())
                .sagaId(generatorRequestAvroModel.getSagaId())
                .customerId(generatorRequestAvroModel.getCustomerId())
                .documentId(generatorRequestAvroModel.getDocumentId())
                .createdAt(generatorRequestAvroModel.getCreatedAt())
                .generationDocumentStatus(GenerationDocumentStatus.valueOf(
                        generatorRequestAvroModel.getDocumentGenerationStatus().name()))
                // Document generation metadata
                .documentType(generatorRequestAvroModel.getDocumentType().name())
                .fileName(generatorRequestAvroModel.getFileName())
                .periodStartDate(generatorRequestAvroModel.getPeriodStartDate())
                .periodEndDate(generatorRequestAvroModel.getPeriodEndDate())
                .totalAmount(generatorRequestAvroModel.getTotalAmount())
                .deliveryAddress(generatorRequestAvroModel.getDeliveryAddress())
                .documentStatus(generatorRequestAvroModel.getDocumentStatus() != null ?
                        generatorRequestAvroModel.getDocumentStatus().name() : null)
                .itemCount(generatorRequestAvroModel.getItemCount())
                .metadata(generatorRequestAvroModel.getMetadata())
                .build();
    }


    public GeneratorResponseAvroModel documentEventPayloadToGeneratorResponseAvroModel(String sagaId, DocumentEventPayload documentEventPayload) {
        return GeneratorResponseAvroModel.newBuilder()
                .setId(documentEventPayload.getGenerationId())
                .setSagaId(sagaId)
                .setGeneratorId(documentEventPayload.getGenerationId())
                .setCustomerId(documentEventPayload.getCustomerId())
                .setDocumentId(documentEventPayload.getDocumentId())
                .setCreatedAt(documentEventPayload.getCreatedAt().toInstant())
                .setGenerationStatus(com.document.notification.system.kafka.document.avro.model.GenerationStatus
                        .valueOf(documentEventPayload.getGenerationStatus()))
                .setFileName(documentEventPayload.getFileName())
                .setContentType(documentEventPayload.getContentType())
                .setContentBase64(documentEventPayload.getContentBase64())
                .setFileSizeInBytes(documentEventPayload.getFileSizeInBytes())
                .setFailureMessages(documentEventPayload.getFailureMessages() != null
                        ? new ArrayList<>(documentEventPayload.getFailureMessages())
                        : new ArrayList<>())
                .build();
    }
}

