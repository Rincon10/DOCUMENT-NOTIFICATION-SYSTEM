package com.document.notification.system.notification.service.domain.outbox.scheduler;

import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.ports.output.message.publisher.NotificationResponseMessagePublisher;
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

@Slf4j
@Component
@AllArgsConstructor
public class DocumentOutboxScheduler implements OutboxScheduler {

    private final DocumentOutboxHelper documentOutboxHelper;
    private final NotificationResponseMessagePublisher notificationResponseMessagePublisher;

    @Override
    @Transactional
    @Scheduled(fixedRateString = "${notification-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${notification-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        log.info("Processing notification outbox messages...");

        Optional<List<DocumentOutboxMessage>> documentOutboxMessageResponse = documentOutboxHelper
                .getDocumentOutboxMessageByOutboxStatus(OutboxStatus.STARTED);

        if (documentOutboxMessageResponse.isPresent() && !documentOutboxMessageResponse.get().isEmpty()) {
            List<DocumentOutboxMessage> documentOutboxMessages = documentOutboxMessageResponse.get();
            log.info("Received {} DocumentOutboxMessage with ids {}, sending to message bus!",
                    documentOutboxMessages.size(),
                    documentOutboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(",")));

            documentOutboxMessages.forEach(documentOutboxMessage -> {
                notificationResponseMessagePublisher.publish(documentOutboxMessage,
                        documentOutboxHelper::updateOutboxMessage);
            });

        } else {
            log.info("No notification outbox message with STARTED status is found for processing!");
        }
    }
}
