package com.document.notification.system.outbox.scheduler.generator;

import com.document.notification.system.outbox.OutboxScheduler;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.ports.output.message.publisher.generator.GenerationRequestMessagePublisher;
import com.document.notification.system.saga.SagaStatus;
import com.document.notification.system.saga.constants.SagaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 25/02/2026
 */


@Slf4j
@Component
@AllArgsConstructor
public class GeneratorOutboxScheduler implements OutboxScheduler {

    private final GeneratorOutboxHelper generatorOutboxHelper;
    private final GenerationRequestMessagePublisher generationRequestMessagePublisher;

    private void updateOutboxStatus(DocumentGenerationOutboxMessage documentGenerationOutboxMessage, OutboxStatus outboxStatus) {
        documentGenerationOutboxMessage.setOutboxStatus(outboxStatus);
        generatorOutboxHelper.save(documentGenerationOutboxMessage);
        log.info("DocumentGenerationOutboxMessage is updated with outbox status: {}", outboxStatus.name());
    }


    @Transactional
    @Scheduled(fixedDelayString = "${document-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${document-service.outbox-scheduler-initial-delay}")
    @Override
    public void processOutboxMessage() {
        Optional<List<DocumentGenerationOutboxMessage>> outboxMessagesResponse = generatorOutboxHelper.getDocumentGenerationOutboxMessagesByTypeAndOutboxStatusAndSagaStatus(
                SagaConstants.SAGA_NAME,
                OutboxStatus.STARTED,
                SagaStatus.STARTED, SagaStatus.COMPENSATING
        );
        if (outboxMessagesResponse.isPresent() && !outboxMessagesResponse.get().isEmpty()) {
            List<DocumentGenerationOutboxMessage> outboxMessages = outboxMessagesResponse.get();
            log.info("Received {} DocumentGenerationOutboxMessage with ids: {}, sending to message bus!",
                    outboxMessages.size(),
                    outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(",")));
            outboxMessages.forEach(outboxMessage ->
                    generationRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));
            log.info("{} OrderPaymentOutboxMessage sent to message bus!", outboxMessages.size());
        }

    }
}
