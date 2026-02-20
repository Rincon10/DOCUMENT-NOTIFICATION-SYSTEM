package com.document.notification.system.kafka.producer.helper;

import com.document.notification.system.outbox.OutboxStatus;
import org.springframework.kafka.support.SendResult;

import java.util.function.BiConsumer;

public interface KafkaProducerHelper {
    <T> T getOrderEventPayload(String payload, Class<T> outputType);

    <T, U> BiConsumer<SendResult<String, T>, Throwable>
    getKafkaCallback(String responseTopicName, T avroModel, U outboxMessage,
                     BiConsumer<U, OutboxStatus> outboxCallback,
                     String documentId, String avroModelName);
}
