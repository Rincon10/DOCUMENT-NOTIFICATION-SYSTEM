package com.document.notification.system.generator.service.domain.outbox.scheduler;

import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.outbox.OutboxScheduler;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public void processOutboxMessage() {
        log.info("Processing document outbox messages...");

        Optional<List<DocumentOutboxMessage>> documentOutboxMessageResponse = documentOutboxHelper.getDocumentOutboxMessageByOutboxStatus(OutboxStatus.STARTED);

    }
}
