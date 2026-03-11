package com.document.notification.system.document.service.messaging.publisher;

import com.document.notification.system.config.DocumentServiceConfigData;
import com.document.notification.system.document.service.messaging.mapper.IDocumentMessagingDataMapper;
import com.document.notification.system.kafka.document.avro.model.NotificationRequestAvroModel;
import com.document.notification.system.kafka.producer.helper.KafkaProducerHelper;
import com.document.notification.system.kafka.producer.service.KafkaProducer;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.ports.output.message.publisher.notification.NotificationRequestMessagePublisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 10/03/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class DocumentNotificationEventRequestKafkaPublisher implements NotificationRequestMessagePublisher {

    private final IDocumentMessagingDataMapper documentMessagingDataMapper;
    private final KafkaProducer<String, NotificationRequestAvroModel> kafkaProducer;
    private final KafkaProducerHelper kafkaProducerHelper;
    private final DocumentServiceConfigData documentServiceConfigData;


    @Override
    public void publish(DocumentNotificationOutboxMessage documentNotificationOutboxMessage, BiConsumer<DocumentNotificationOutboxMessage, OutboxStatus> outboxCallback) {

        DocumentNotificationEventPayload documentNotificationEventPayload = kafkaProducerHelper
                .getDocumentEventPayload(documentNotificationOutboxMessage.getPayload(), DocumentNotificationEventPayload.class);

        String sagaId = documentNotificationOutboxMessage.getSagaId().toString();
        log.info("Received DocumentNotificationOutboxMessage for document id: {} and saga id: {}",
                documentNotificationEventPayload.getDocumentId(),
                sagaId);

        try {
            NotificationRequestAvroModel notificationRequestAvroModel = documentMessagingDataMapper
                    .documentNotificationEventPayloadToNotificationRequestAvroModel(sagaId, documentNotificationEventPayload);

            kafkaProducer.send(documentServiceConfigData.getNotificationRequestTopicName(),
                    sagaId,
                    notificationRequestAvroModel,
                    kafkaProducerHelper.getKafkaCallback(documentServiceConfigData.getNotificationRequestTopicName(),
                            notificationRequestAvroModel,
                            documentNotificationOutboxMessage,
                            outboxCallback,
                            documentNotificationEventPayload.getDocumentId(),
                            NotificationRequestAvroModel.class.getTypeName()));

            log.info("DocumentNotificationOutboxMessage sent to kafka for document id: {} and saga id: {}",
                    documentNotificationEventPayload.getDocumentId(),
                    sagaId);
        } catch (Exception e) {
            log.error("Error while sending DocumentNotificationOutboxMessage to kafka for document id: {} and saga id: {}, error: {}",
                    documentNotificationEventPayload.getDocumentId(),
                    sagaId,
                    e.getMessage(), e);
        }

    }
}
