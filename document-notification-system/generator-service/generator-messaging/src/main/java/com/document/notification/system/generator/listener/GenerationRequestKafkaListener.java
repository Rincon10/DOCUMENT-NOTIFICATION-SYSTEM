package com.document.notification.system.generator.listener;

import com.document.notification.system.generator.mapper.DocumentMessagingDataMapper;
import com.document.notification.system.generator.service.domain.exception.GeneratorDomainException;
import com.document.notification.system.generator.service.domain.ports.input.message.listener.GenerationRequestMessageListener;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.DocumentGenerationStatus;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
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
    private final DocumentMessagingDataMapper documentMessagingDataMapper;


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
        messages.forEach(generatorRequestAvroModel -> {
            log.info("Received message with key: {} from partition: {} with offset: {} for document id: {} and saga id: {}",
                    keys.get(messages.indexOf(generatorRequestAvroModel)),
                    partitions.get(messages.indexOf(generatorRequestAvroModel)),
                    offsets.get(messages.indexOf(generatorRequestAvroModel)),
                    generatorRequestAvroModel.getDocumentId(),
                    generatorRequestAvroModel.getSagaId());
            try {

                DocumentGenerationStatus documentGenerationStatus = generatorRequestAvroModel.getDocumentGenerationStatus();
                if (DocumentGenerationStatus.PENDING.equals(documentGenerationStatus)) {
                    log.info("Processing pending generation request for document id: {} and saga id: {}",
                            generatorRequestAvroModel.getDocumentId(),
                            generatorRequestAvroModel.getSagaId());
                    generationRequestMessageListener.completedGeneration(documentMessagingDataMapper.generatorRequestAvroModelToGenerationRequest(generatorRequestAvroModel));
                } else if (DocumentGenerationStatus.CANCELLED.equals(documentGenerationStatus)) {
                    log.info("Processing cancelled generation request for document id: {} and saga id: {}",
                            generatorRequestAvroModel.getDocumentId(),
                            generatorRequestAvroModel.getSagaId());
                    //generationRequestMessageListener.cancellGeneration(documentMessagingDataMapper.generatorRequestAvroModelToGenerationRequest(generatorRequestAvroModel));
                } else {
                    log.warn("Received message with unknown document generation status: {} for document id: {} and saga id: {}",
                            documentGenerationStatus,
                            generatorRequestAvroModel.getDocumentId(),
                            generatorRequestAvroModel.getSagaId());
                }

            } catch (DataAccessException e) {
                SQLException sqlException = (SQLException) e.getRootCause();
                if (sqlException != null && sqlException.getSQLState() != null &&
                        PSQLState.UNIQUE_VIOLATION.getState().equals(sqlException.getSQLState())) {
                    //NO-OP for unique constraint exception
                    log.error("Caught unique constraint exception with sql state: {} " +
                                    "in GenerationRequestKafkaListener for document id: {}",
                            sqlException.getSQLState(), generatorRequestAvroModel.getDocumentId());
                } else {
                    throw new GeneratorDomainException("Throwing DataAccessException in" +
                            " PaymentRequestKafkaListener: " + e.getMessage(), e);
                }
            }

        });
    }
}
