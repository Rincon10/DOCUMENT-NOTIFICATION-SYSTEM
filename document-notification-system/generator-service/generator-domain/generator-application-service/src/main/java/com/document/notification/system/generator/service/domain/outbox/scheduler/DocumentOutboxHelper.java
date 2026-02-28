package com.document.notification.system.generator.service.domain.outbox.scheduler;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.exception.GeneratorDomainException;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.document.notification.system.saga.constants.SagaConstants.SAGA_NAME;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Component
@Slf4j
@AllArgsConstructor
public class DocumentOutboxHelper {
    private final DocumentOutboxRepository documentOutboxRepository;


    @Transactional(readOnly = true)
    public Optional<DocumentOutboxMessage> getCompletedDocumentOutboxMessageBySagaIdAndGenerationStatus(UUID sagaId,
                                                                                                        GenerationStatus
                                                                                                                generationStatus) {
        return documentOutboxRepository.findByTypeAndSagaIdAndGenerationStatusAndOutboxStatus(SAGA_NAME, sagaId, generationStatus, OutboxStatus.COMPLETED);
    }

    @Transactional
    public void updateOutboxMessage(DocumentOutboxMessage documentOutboxMessage, OutboxStatus outboxStatus) {
        documentOutboxMessage.setOutboxStatus(outboxStatus);
        save(documentOutboxMessage);
        log.info("Document outbox table status is updated as: {}", outboxStatus.name());
    }

    private void save(DocumentOutboxMessage documentOutboxMessage) {
        DocumentOutboxMessage response = documentOutboxRepository.save(documentOutboxMessage);
        if (response == null) {
            log.error("Could not save DocumentOutboxMessage!");
            throw new GeneratorDomainException("Could not save DocumentOutboxMessage!");
        }
        log.info("DocumentOutboxMessage is saved with id: {}", documentOutboxMessage.getId());
    }
}
