package com.document.notification.system.generator.publisher.kafka;

import com.document.notification.system.generator.mapper.GeneratorMessagingDataMapper;
import com.document.notification.system.generator.service.domain.config.GenerationServiceConfigData;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import com.document.notification.system.kafka.producer.helper.KafkaProducerHelper;
import com.document.notification.system.kafka.producer.service.KafkaProducer;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class GenerationResponseKafkaPublisher implements GenerationResponseMessagePublisher {

    private final KafkaProducerHelper kafkaProducerHelper;
    private final GenerationServiceConfigData generationServiceConfigData;
    private final KafkaProducer<String, GeneratorResponseAvroModel> kafkaProducer;
    private final GeneratorMessagingDataMapper generatorMessagingDataMapper;


    @Override
    public void publish(DocumentOutboxMessage documentOutboxMessage, BiConsumer<DocumentOutboxMessage, OutboxStatus> outboxCallback) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
