package com.document.notification.system.document.service.messaging.listener.kafka;

import com.document.notification.system.document.service.messaging.mapper.IDocumentMessagingDataMapper;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;
import com.document.notification.system.ports.input.message.listener.generator.GenerationResponseMessageListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */

@Slf4j
@Component
@AllArgsConstructor
public class GenerationResponseKafkaListener implements KafkaConsumer<GeneratorResponseAvroModel> {

    private final GenerationResponseMessageListener generationResponseMessageListener;
    private final IDocumentMessagingDataMapper documentMessagingDataMapper;


    @KafkaListener(id = "${kafka-consumer-config.generator-consumer-group-id}", topics = "${document-service.generator-response-topic-name}")
    @Override
    public void receive(@Payload List<GeneratorResponseAvroModel> messages, @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of generations responses received with keys:{}, partitions:{} and offsets: {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(generatorResponseAvroModel -> {
            try {
                GenerationResponse generationResponse = documentMessagingDataMapper
                        .generatorResponseAvroModelToGenerationResponse(generatorResponseAvroModel);

                if (generationResponse.getGenerationStatus().isSuccessful()) {
                    log.info("Processing successful generation for document id: {}", generatorResponseAvroModel.getDocumentId());
                    generationResponseMessageListener.generationCompleted(generationResponse);
                } else if (!generationResponse.getGenerationStatus().isSuccessful()) {
                    log.info("Processing unsuccessful generation for document id: {}", generatorResponseAvroModel.getDocumentId());
                    generationResponseMessageListener.generationCancelled(generationResponse);
                }
            } catch (OptimisticLockingFailureException e) {
                //NO-OP for optimistic lock. This means another thread finished the work, do not throw error to prevent reading the data from kafka again!
                log.error("Caught optimistic locking exception in GenerationResponseKafkaListener for document id: {}",
                        generatorResponseAvroModel.getDocumentId());
            } catch (Exception e) {
                log.error("Error processing generation response for document id: {}, error: {}",
                        generatorResponseAvroModel.getDocumentId(), e.getMessage(), e);
            }
        });

    }
}
