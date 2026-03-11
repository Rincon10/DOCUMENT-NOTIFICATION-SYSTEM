package com.document.notification.system.outbox.scheduler.notification;

import com.document.notification.system.outbox.OutboxScheduler;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.ports.output.message.publisher.notification.NotificationRequestMessagePublisher;
import com.document.notification.system.saga.SagaStatus;
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
 * @since 10/03/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class NotificationOutboxScheduler implements OutboxScheduler {

    private final NotificationOutboxHelper notificationOutboxHelper;
    private final NotificationRequestMessagePublisher notificationRequestMessagePublisher;

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${document-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${document-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {

        Optional<List<DocumentNotificationOutboxMessage>> documentNotificationOutboxMessages = notificationOutboxHelper.getNotificationOutboxMessageByOutboxStatusAndSagaStatus(
                OutboxStatus.STARTED,
                SagaStatus.PROCESSING
        );

        if (documentNotificationOutboxMessages.isPresent() && !documentNotificationOutboxMessages.get().isEmpty()) {
            List<DocumentNotificationOutboxMessage> outboxMessages = documentNotificationOutboxMessages.get();
            log.info("Received {} DocumentNotificationOutboxMessage with ids: {}, sending to message bus!",
                    outboxMessages.size(),
                    outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()
                    ).collect(Collectors.joining(",")));
            outboxMessages.forEach(outboxMessage ->
                    notificationRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));
            log.info("{} DocumentNotificationOutboxMessage sent to message bus!", outboxMessages.size());
        }

    }

    private void updateOutboxStatus(DocumentNotificationOutboxMessage documentNotificationOutboxMessage, OutboxStatus outboxStatus) {
        documentNotificationOutboxMessage.setOutboxStatus(outboxStatus);
        notificationOutboxHelper.save(documentNotificationOutboxMessage);
        log.info("DocumentNotificationOutboxMessage is updated with outbox status: {}", outboxStatus.name());
    }
}
