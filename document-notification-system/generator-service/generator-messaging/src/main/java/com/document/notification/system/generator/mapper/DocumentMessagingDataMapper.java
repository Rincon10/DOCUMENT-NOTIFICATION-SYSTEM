package com.document.notification.system.generator.mapper;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.valueobject.GenerationDocumentStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Mapper for converting between Avro models and domain DTOs
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
public class DocumentMessagingDataMapper {

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


}

