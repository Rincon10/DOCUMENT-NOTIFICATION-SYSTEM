package com.document.notification.system.document.service.messaging.listener.kafka;

import com.document.notification.system.document.service.messaging.mapper.IDocumentMessagingDataMapper;
import com.document.notification.system.dto.message.NotificationResponse;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.NotificationResponseAvroModel;
import com.document.notification.system.kafka.document.avro.model.NotificationStatus;
import com.document.notification.system.ports.input.message.listener.notification.NotificationResponseMessageListener;
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
 * @since 19/03/2026
 */

@Slf4j
@Component
@AllArgsConstructor
public class NotificationResponseKafkaListener implements KafkaConsumer<NotificationResponseAvroModel> {

    private final NotificationResponseMessageListener notificationResponseMessageListener;
    private final IDocumentMessagingDataMapper documentMessagingDataMapper;

    @KafkaListener(id = "${kafka-consumer-config.notification-consumer-group-id}", topics = "${document-service.notification-response-topic-name}")
    @Override
    public void receive(@Payload List<NotificationResponseAvroModel> messages, @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of notification responses received with keys:{}, partitions:{} and offsets: {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(notificationResponseAvroModel -> {
            try {
                NotificationResponse notificationResponse = documentMessagingDataMapper
                        .notificationResponseAvroModelToNotificationResponse(notificationResponseAvroModel);

                if (NotificationStatus.NOTIFICATION_SENT == notificationResponseAvroModel.getNotificationStatus()) {
                    log.info("Processing successful notification for document id: {}", notificationResponseAvroModel.getDocumentId());
                    notificationResponseMessageListener.notificationCompleted(notificationResponse);
                } else {
                    log.info("Processing unsuccessful notification for document id: {}", notificationResponseAvroModel.getDocumentId());
                    notificationResponseMessageListener.notificationFailed(notificationResponse);
                }
            } catch (OptimisticLockingFailureException e) {
                //NO-OP for optimistic lock. This means another thread finished the work, do not throw error to prevent reading the data from kafka again!
                log.error("Caught optimistic locking exception in NotificationResponseKafkaListener for document id: {}",
                        notificationResponseAvroModel.getDocumentId());
            } catch (Exception e) {
                log.error("Error processing notification response for document id: {}, error: {}",
                        notificationResponseAvroModel.getDocumentId(), e.getMessage(), e);
            }
        });
    }
}
