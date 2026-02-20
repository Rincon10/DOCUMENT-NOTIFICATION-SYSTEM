package com.document.notification.system.document.service.messaging.listener.kafka;

import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */

@Slf4j
@Component
public class GenerationResponseKafkaListener implements KafkaConsumer<GeneratorResponseAvroModel> {

    @KafkaListener(id = "${kafka-consumer-config.payment-consumer-group-id}", topics = "${order-service.payment-response-topic-name}")
    @Override
    public void receive(List<GeneratorResponseAvroModel> messages, List<String> keys, List<Integer> partitions, List<Long> offsets) {

    }
}
