package com.document.notification.system.document.service.messaging.publisher;

import com.document.notification.system.config.DocumentServiceConfigData;
import com.document.notification.system.document.service.messaging.mapper.IDocumentMessagingDataMapper;
import com.document.notification.system.kafka.document.avro.model.GeneratorRequestAvroModel;
import com.document.notification.system.kafka.producer.helper.KafkaProducerHelper;
import com.document.notification.system.kafka.producer.service.KafkaProducer;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.ports.output.message.publisher.generator.GenerationRequestMessagePublisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */


@Slf4j
@Component
@AllArgsConstructor
public class DocumentGenerationEventRequestKafkaPublisher implements GenerationRequestMessagePublisher {

    private final IDocumentMessagingDataMapper documentMessagingDataMapper;
    private final KafkaProducer<String, GeneratorRequestAvroModel> kafkaProducer;
    private final KafkaProducerHelper kafkaProducerHelper;
    private final DocumentServiceConfigData documentServiceConfigData;

    @Override
    public void publish(DocumentGenerationOutboxMessage documentGenerationOutboxMessage, BiConsumer<DocumentGenerationOutboxMessage, OutboxStatus> outboxCallback) {
        DocumentGenerationEventPayload documentGenerationEventPayload = kafkaProducerHelper.getOrderEventPayload(documentGenerationOutboxMessage.getPayload(), DocumentGenerationEventPayload.class);

        String sagaId = documentGenerationOutboxMessage.getSagaId().toString();
        log.info("Received DocumentGenerationOutboxMessage for document id: {} and saga id: {}",
                documentGenerationEventPayload.getDocumentId(),
                sagaId);

        try{
            GeneratorRequestAvroModel generatorRequestAvroModel = documentMessagingDataMapper
                    .documentGenerationEventPayloadToGeneratorRequestAvroModel(sagaId,documentGenerationEventPayload);

            kafkaProducer.send(documentServiceConfigData.getGeneratorRequestTopicName(),
                    sagaId,
                    generatorRequestAvroModel,
                    kafkaProducerHelper.getKafkaCallback(documentServiceConfigData.getGeneratorRequestTopicName(),
                            generatorRequestAvroModel,
                            documentGenerationOutboxMessage,
                            outboxCallback,
                            documentGenerationEventPayload.getDocumentId(),
                            GeneratorRequestAvroModel.class.getTypeName()));

            log.info("DocumentGenerationOutboxMessage sent to kafka for document id: {} and saga id: {}",
                    documentGenerationEventPayload.getDocumentId(),
                    sagaId);
        } catch (Exception e) {
            log.error("Error while sending DocumentGenerationOutboxMessage to kafka for document id: {} and saga id: {}," +
                    " error: {}", documentGenerationEventPayload.getDocumentId(), sagaId, e.getMessage());
        }


    }

}
