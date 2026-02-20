package com.document.notification.system.kafka.producer.helper.impl;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.kafka.producer.helper.KafkaProducerHelper;
import com.document.notification.system.outbox.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Component
@Slf4j
@AllArgsConstructor
public class KafkaProducerHelperImpl implements KafkaProducerHelper {
    private final ObjectMapper objectMapper;

    @Override
    public <T> T getOrderEventPayload(String payload, Class<T> outputType) {
        try {
            return objectMapper.readValue(payload, outputType);
        } catch (JsonProcessingException e) {
            log.error("Could not read {} object!", outputType.getName(), e);
            throw new DocumentDomainException("Could not read " + outputType.getName() + " object!", e);
        }
    }

    @Override
    public <T, U> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(String responseTopicName, T avroModel, U outboxMessage, BiConsumer<U, OutboxStatus> outboxCallback, String documentId, String avroModelName) {
        return (result, ex) -> {
            if (ex == null) {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("Received successful response from Kafka for document id: {}" +
                                " Topic: {} Partition: {} Offset: {} Timestamp: {}",
                        documentId,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp());
                outboxCallback.accept(outboxMessage, OutboxStatus.COMPLETED);
            } else {
                log.error("Error while sending {} with message: {} and outbox type: {} to topic {}",
                        avroModelName, avroModel.toString(), outboxMessage.getClass().getName(), responseTopicName, ex);
                outboxCallback.accept(outboxMessage, OutboxStatus.FAILED);
            }
        };
    }
}
