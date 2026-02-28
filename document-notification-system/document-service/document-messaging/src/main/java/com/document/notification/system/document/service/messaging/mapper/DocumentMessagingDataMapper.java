package com.document.notification.system.document.service.messaging.mapper;

import com.document.notification.system.dto.message.CustomerModel;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.kafka.document.avro.model.CustomerAvroModel;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Component
public class DocumentMessagingDataMapper implements IDocumentMessagingDataMapper{
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
        return GeneratorRequestAvroModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setSagaId(sagaId)
                .setCustomerId(documentGenerationEventPayload.getCustomerId())
                .setDocumentId(documentGenerationEventPayload.getDocumentId())
                .setCreatedAt(Instant.from(documentGenerationEventPayload.getCreatedAt()))
                .setDocumentGenerationStatus(
                        com.document.notification.system.kafka.document.avro.model.DocumentGenerationStatus
                                .valueOf(documentGenerationEventPayload.getDocumentGenerationStatus())
                )
                .build();
    }
}
