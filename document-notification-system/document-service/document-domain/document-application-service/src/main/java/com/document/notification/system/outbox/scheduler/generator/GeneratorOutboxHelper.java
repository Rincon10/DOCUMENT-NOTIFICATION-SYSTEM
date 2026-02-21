package com.document.notification.system.outbox.scheduler.generator;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.utils.JsonSerializationUtil;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.ports.output.repository.GeneratorOutboxRepository;
import com.document.notification.system.saga.SagaStatus;
import com.document.notification.system.saga.constants.SagaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;


/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GeneratorOutboxHelper {
    private final GeneratorOutboxRepository generatorOutboxRepository;


    @Transactional
    public void saveGenerationOutboxMessage(DocumentGenerationEventPayload documentGenerationEventPayload,
                                            DocumentStatus documentStatus,
                                            SagaStatus sagaStatus,
                                            OutboxStatus outboxStatus,
                                            UUID sagaId) {

        String payload = createPayload(documentGenerationEventPayload);

        DocumentGenerationOutboxMessage documentGenerationOutboxMessage =
                DocumentGenerationOutboxMessage.builder()
                        .id(UUID.randomUUID())
                        .sagaId(sagaId)
                        .createdAt(documentGenerationEventPayload.getCreatedAt())
                        .type(SagaConstants.SAGA_NAME)
                        .payload(payload)
                        .documentStatus(documentStatus)
                        .sagaStatus(sagaStatus)
                        .outboxStatus(outboxStatus)
                        .build();
        save(documentGenerationOutboxMessage);
    }

    @Transactional
    public void save(DocumentGenerationOutboxMessage documentGenerationOutboxMessage) {
        DocumentGenerationOutboxMessage response = generatorOutboxRepository.save(documentGenerationOutboxMessage);
        if (Objects.isNull(response)) {
            log.error("Could not save DocumentGenerationOutboxMessage with outbox id: {}", documentGenerationOutboxMessage.getId());
            throw new DocumentDomainException("Could not save DocumentGenerationOutboxMessage with outbox id: " +
                    documentGenerationOutboxMessage.getId());
        }
        log.info("DocumentGenerationOutboxMessage saved with outbox id: {}", documentGenerationOutboxMessage.getId());
    }

    private String createPayload(DocumentGenerationEventPayload documentGenerationEventPayload) {
        String errorMessage = "Could not create DocumentGenerationEventPayload object for document id: " +
                documentGenerationEventPayload.getDocumentId();
        return JsonSerializationUtil.toJson(documentGenerationEventPayload, errorMessage);
    }
}
