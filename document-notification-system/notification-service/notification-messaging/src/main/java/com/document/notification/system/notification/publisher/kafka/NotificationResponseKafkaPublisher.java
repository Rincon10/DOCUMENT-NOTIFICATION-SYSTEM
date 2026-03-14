package com.document.notification.system.notification.publisher.kafka;

import com.document.notification.system.notification.mapper.NotificationMessagingDataMapper;
import com.document.notification.system.notification.service.domain.config.NotificationServiceConfigData;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.ports.output.message.publisher.NotificationResponseMessagePublisher;
import com.document.notification.system.kafka.document.avro.model.NotificationResponseAvroModel;
import com.document.notification.system.kafka.producer.helper.KafkaProducerHelper;
import com.document.notification.system.kafka.producer.service.KafkaProducer;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@AllArgsConstructor
public class NotificationResponseKafkaPublisher implements NotificationResponseMessagePublisher {

    private final KafkaProducerHelper kafkaProducerHelper;
    private final NotificationServiceConfigData notificationServiceConfigData;
    private final KafkaProducer<String, NotificationResponseAvroModel> kafkaProducer;
    private final NotificationMessagingDataMapper notificationMessagingDataMapper;

    @Override
    public void publish(DocumentOutboxMessage documentOutboxMessage, BiConsumer<DocumentOutboxMessage, OutboxStatus> outboxCallback) {

        DocumentEventPayload documentEventPayload = kafkaProducerHelper.getDocumentEventPayload(
                documentOutboxMessage.getPayload(), DocumentEventPayload.class);

        String sagaId = documentOutboxMessage.getSagaId().toString();
        log.info("Received DocumentOutboxMessage for document id: {} and saga id: {}",
                documentEventPayload.getDocumentId(),
                sagaId);

        try {
            NotificationResponseAvroModel notificationResponseAvroModel = notificationMessagingDataMapper
                    .documentEventPayloadToNotificationResponseAvroModel(sagaId, documentEventPayload);

            kafkaProducer.send(notificationServiceConfigData.getNotificationResponseTopicName(),
                    sagaId,
                    notificationResponseAvroModel,
                    kafkaProducerHelper.getKafkaCallback(
                            notificationServiceConfigData.getNotificationResponseTopicName(),
                            notificationResponseAvroModel,
                            documentOutboxMessage,
                            outboxCallback,
                            documentEventPayload.getDocumentId(),
                            "NotificationResponseAvroModel"));

            log.info("NotificationResponseAvroModel sent to kafka for document id: {} and saga id: {}",
                    notificationResponseAvroModel.getDocumentId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending NotificationResponseAvroModel message" +
                            " to kafka with document id: {} and saga id: {}, error: {}",
                    documentEventPayload.getDocumentId(), sagaId, e.getMessage());
        }
    }
}
