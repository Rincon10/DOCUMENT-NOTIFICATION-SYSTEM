package com.document.notification.system.notification.listener;

import com.document.notification.system.notification.mapper.NotificationMessagingDataMapper;
import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import com.document.notification.system.notification.service.domain.ports.input.message.listener.NotificationRequestMessageListener;
import com.document.notification.system.kafka.consumer.KafkaConsumer;
import com.document.notification.system.kafka.document.avro.model.DocumentNotificationStatus;
import com.document.notification.system.kafka.document.avro.model.NotificationRequestAvroModel;
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

@Slf4j
@Component
@AllArgsConstructor
public class NotificationRequestKafkaListener implements KafkaConsumer<NotificationRequestAvroModel> {

    private final NotificationRequestMessageListener notificationRequestMessageListener;
    private final NotificationMessagingDataMapper notificationMessagingDataMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.notification-consumer-group-id}",
            topics = "${notification-service.notification-request-topic-name}")
    public void receive(@Payload List<NotificationRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of notification requests received with keys:{}, partitions:{} and offsets: {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(notificationRequestAvroModel -> {
            log.info("Received notification message with key: {} for document id: {} and saga id: {}",
                    keys.get(messages.indexOf(notificationRequestAvroModel)),
                    notificationRequestAvroModel.getDocumentId(),
                    notificationRequestAvroModel.getSagaId());
            try {
                DocumentNotificationStatus documentNotificationStatus = notificationRequestAvroModel
                        .getDocumentNotificationStatus();

                if (DocumentNotificationStatus.GENERATED.equals(documentNotificationStatus)) {
                    log.info("Processing notification request for document id: {} and saga id: {}",
                            notificationRequestAvroModel.getDocumentId(),
                            notificationRequestAvroModel.getSagaId());
                    notificationRequestMessageListener.processNotification(
                            notificationMessagingDataMapper.notificationRequestAvroModelToNotificationRequest(
                                    notificationRequestAvroModel));
                } else {
                    log.warn("Received message with unknown document notification status: {} for document id: {} and saga id: {}",
                            documentNotificationStatus,
                            notificationRequestAvroModel.getDocumentId(),
                            notificationRequestAvroModel.getSagaId());
                }

            } catch (DataAccessException e) {
                SQLException sqlException = (SQLException) e.getRootCause();
                if (sqlException != null && sqlException.getSQLState() != null &&
                        PSQLState.UNIQUE_VIOLATION.getState().equals(sqlException.getSQLState())) {
                    log.error("Caught unique constraint exception with sql state: {} " +
                                    "in NotificationRequestKafkaListener for document id: {}",
                            sqlException.getSQLState(), notificationRequestAvroModel.getDocumentId());
                } else {
                    throw new NotificationDomainException("Throwing DataAccessException in" +
                            " NotificationRequestKafkaListener: " + e.getMessage(), e);
                }
            }
        });
    }
}
