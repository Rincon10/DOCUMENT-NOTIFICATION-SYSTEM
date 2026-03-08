package com.document.notification.system.generator.service.domain.outbox.scheduler;

import com.document.notification.system.outbox.OutboxScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */

@Slf4j
@Component
public class DocumentOutboxScheduler implements OutboxScheduler {
    @Override
    public void processOutboxMessage() {
        log.info("Processing document outbox messages...");
        // Here you would implement the logic to fetch and process outbox messages
    }
}
