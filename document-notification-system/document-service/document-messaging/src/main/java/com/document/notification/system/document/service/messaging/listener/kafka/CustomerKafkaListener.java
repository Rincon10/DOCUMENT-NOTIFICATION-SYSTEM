package com.document.notification.system.document.service.messaging.listener.kafka;

import com.document.notification.system.document.service.messaging.mapper.IDocumentMessagingDataMapper;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.CustomerAvroModel;
import com.document.notification.system.ports.input.message.listener.customer.ICustomerMessageListener;
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
 * @since 21/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class CustomerKafkaListener implements KafkaConsumer<CustomerAvroModel> {

    private final IDocumentMessagingDataMapper mapper;
    private final ICustomerMessageListener customerMessageListener;


    @Override
    @KafkaListener(id = "${kafka-consumer-config.customer-group-id}", topics = "${document-service.customer-topic-name}")
    public void receive(@Payload List<CustomerAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} number of customer create messages received with keys {}, partitions {} and offsets {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(customerAvroModel ->
                customerMessageListener.customerCreated(mapper
                        .customerAvroModeltoCustomerModel(customerAvroModel)));

    }
}
