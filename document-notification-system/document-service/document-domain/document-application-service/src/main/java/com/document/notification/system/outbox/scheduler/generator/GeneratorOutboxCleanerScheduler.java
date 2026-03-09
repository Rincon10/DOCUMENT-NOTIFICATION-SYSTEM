package com.document.notification.system.outbox.scheduler.generator;

import com.document.notification.system.outbox.OutboxScheduler;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.saga.SagaStatus;
import com.document.notification.system.saga.constants.SagaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 8/03/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GeneratorOutboxCleanerScheduler implements OutboxScheduler {

    private final GeneratorOutboxHelper generatorOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<DocumentGenerationOutboxMessage>> documentGenerationOutboxMessages = generatorOutboxHelper.getDocumentGenerationOutboxMessagesByTypeAndOutboxStatusAndSagaStatus(SagaConstants.SAGA_NAME, OutboxStatus.COMPLETED, SagaStatus.FAILED,
                SagaStatus.COMPENSATED, SagaStatus.SUCCESSFUL);

        if (documentGenerationOutboxMessages.isPresent()) {
            List<DocumentGenerationOutboxMessage> outboxMessages = documentGenerationOutboxMessages.get();
            log.info("Received {} DocumentGenerationOutboxMessage for clean-up. The payloads: {}",
                    outboxMessages.size(),
                    outboxMessages.stream().map(DocumentGenerationOutboxMessage::getPayload)
                            .collect(Collectors.joining("\n")));
            generatorOutboxHelper.deletePaymentOutboxMessageByOutboxStatusAndSagaStatus(SagaConstants.SAGA_NAME, OutboxStatus.COMPLETED, SagaStatus.FAILED,
                    SagaStatus.COMPENSATED, SagaStatus.SUCCESSFUL);
            log.info("{} DocumentGenerationOutboxMessage are deleted from database!", outboxMessages.size());
        }

    }
}
