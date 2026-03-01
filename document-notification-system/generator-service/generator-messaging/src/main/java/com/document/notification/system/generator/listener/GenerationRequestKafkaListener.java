package com.document.notification.system.generator.listener;

import com.document.notification.system.generator.service.domain.ports.input.message.listener.GenerationRequestMessageListener;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GenerationRequestKafkaListener implements KafkaConsumer<GeneratorRequestAvroModel> {

    private final GenerationRequestMessageListener generationRequestMessageListener;


    @Override
    @KafkaListener(id = "${kafka-consumer-config.generator-consumer-group-id}",
            topics = "${generator-service.generator-request-topic-name}")
    public void receive(@Payload List<GeneratorRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of generation requests received with keys:{}, partitions:{} and offsets: {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());
        messages.forEach(message -> {
            log.info("Received message with key: {} from partition: {} with offset: {} for document id: {} and saga id: {}",
                    keys.get(messages.indexOf(message)),
                    partitions.get(messages.indexOf(message)),
                    offsets.get(messages.indexOf(message)),
                    message.getDocumentId(),
                    message.getSagaId());
        });
    }
}
