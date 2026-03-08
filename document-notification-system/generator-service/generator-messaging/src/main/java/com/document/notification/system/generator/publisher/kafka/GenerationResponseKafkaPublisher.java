package com.document.notification.system.generator.publisher.kafka;

import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
@Slf4j
@Component
public class GenerationResponseKafkaPublisher implements GenerationResponseMessagePublisher {
    @Override
    public void publish(DocumentOutboxMessage documentOutboxMessage, BiConsumer<DocumentOutboxMessage, OutboxStatus> outboxCallback) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
