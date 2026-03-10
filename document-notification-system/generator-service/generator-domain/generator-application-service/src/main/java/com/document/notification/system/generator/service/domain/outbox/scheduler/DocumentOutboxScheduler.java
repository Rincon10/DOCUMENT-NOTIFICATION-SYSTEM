package com.document.notification.system.generator.service.domain.outbox.scheduler;

import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.outbox.OutboxScheduler;
import com.document.notification.system.outbox.OutboxStatus;
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
 * @since 7/03/2026
 */

@Slf4j
@Component
@AllArgsConstructor
public class DocumentOutboxScheduler implements OutboxScheduler {

    private final DocumentOutboxHelper documentOutboxHelper;
    private final GenerationResponseMessagePublisher generationResponseMessagePublisher;

    @Override
    @Transactional
    @Scheduled(fixedRateString = "${generator-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${generator-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        log.info("Processing document outbox messages...");

        Optional<List<DocumentOutboxMessage>> documentOutboxMessageResponse = documentOutboxHelper.getDocumentOutboxMessageByOutboxStatus(OutboxStatus.STARTED);

        if (documentOutboxMessageResponse.isPresent() && !documentOutboxMessageResponse.get().isEmpty()) {
            List<DocumentOutboxMessage> documentOutboxMessages = documentOutboxMessageResponse.get();
            log.info("Received {} DocumentOutboxMessage with ids {}, sending to message bus!", documentOutboxMessages.size(),
                    documentOutboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(",")));

            documentOutboxMessages.forEach(documentOutboxMessage -> {
                generationResponseMessagePublisher.publish(documentOutboxMessage, documentOutboxHelper::updateOutboxMessage);
            });

        } else {
            log.info("No document outbox message with STARTED status is found for processing!");
        }


    }
}
