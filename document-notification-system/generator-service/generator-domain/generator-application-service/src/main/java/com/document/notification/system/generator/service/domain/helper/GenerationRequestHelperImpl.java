package com.document.notification.system.generator.service.domain.helper;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.mapper.GenerationDataMapper;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.outbox.scheduler.DocumentOutboxHelper;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import com.document.notification.system.generator.service.domain.service.IGeneratorDomainService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GenerationRequestHelperImpl implements GenerationRequestHelper {
    private final IGeneratorDomainService generatorDomainService;
    private final GenerationDataMapper generationDataMapper;
    private final DocumentGenerationRepository documentGenerationRepository;
    private final DocumentOutboxHelper documentOutboxHelper;
    private final GenerationResponseMessagePublisher generationResponseMessagePublisher;

    @Transactional
    @Override
    public void persistGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        if (publishIfOutboxMessageProcessedForGeneration(generationRequest, GenerationStatus.GENERATION_COMPLETED)) {
            log.info("An outbox message with saga id: {} is already saved to database!",
                    generationRequest.getSagaId());
            return;
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }


    private boolean publishIfOutboxMessageProcessedForGeneration(GenerationRequest generationRequest, GenerationStatus generationStatus) {
        Optional<DocumentOutboxMessage> documentOutboxMessagesOptional = documentOutboxHelper.getCompletedDocumentOutboxMessageBySagaIdAndGenerationStatus(UUID.fromString(generationRequest.getSagaId()), generationStatus);
        if (documentOutboxMessagesOptional.isPresent()) {
            DocumentOutboxMessage documentOutboxMessage = documentOutboxMessagesOptional.get();
            log.info("An outbox message with saga id: {} is already saved to database with generation status: {}!",
                    generationRequest.getSagaId(),
                    generationStatus);
            generationResponseMessagePublisher.publish(documentOutboxMessagesOptional.get(), documentOutboxHelper::updateOutboxMessage);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void persistCancelledGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
