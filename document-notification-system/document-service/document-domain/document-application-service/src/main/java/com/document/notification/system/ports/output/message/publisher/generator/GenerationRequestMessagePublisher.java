package com.document.notification.system.ports.output.message.publisher.generator;

import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;

import java.util.function.BiConsumer;

public interface GenerationRequestMessagePublisher {

    void publish(DocumentGenerationOutboxMessage documentGenerationOutboxMessage,
                 BiConsumer<DocumentGenerationOutboxMessage, OutboxStatus> outboxCallback);
}
