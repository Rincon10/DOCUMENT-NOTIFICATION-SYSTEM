package com.document.notification.system.generator.mapper;

import com.document.notification.system.domain.valueobject.GenerationDocumentStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Component
public class DocumentMessagingDataMapper {
    public GenerationRequest generatorRequestAvroModelToGenerationRequest(GeneratorRequestAvroModel generatorRequestAvroModel) {
        return GenerationRequest.builder()
                .id(generatorRequestAvroModel.getId())
                .sagaId(generatorRequestAvroModel.getSagaId())
                .customerId(generatorRequestAvroModel.getCustomerId())
                .documentId(generatorRequestAvroModel.getDocumentId())
                .createdAt(generatorRequestAvroModel.getCreatedAt())
                .generationDocumentStatus(GenerationDocumentStatus.valueOf(generatorRequestAvroModel.getDocumentGenerationStatus().name()))
                .build();
    }
}
