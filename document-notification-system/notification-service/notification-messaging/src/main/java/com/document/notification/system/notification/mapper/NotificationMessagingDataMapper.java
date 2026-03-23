package com.document.notification.system.notification.mapper;

import com.document.notification.system.domain.valueobject.DocumentNotificationStatus;
import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.kafka.document.avro.model.NotificationRequestAvroModel;
import com.document.notification.system.kafka.document.avro.model.NotificationResponseAvroModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
public class NotificationMessagingDataMapper {

    public NotificationRequest notificationRequestAvroModelToNotificationRequest(NotificationRequestAvroModel avroModel) {
        return NotificationRequest.builder()
                .id(avroModel.getId())
                .sagaId(avroModel.getSagaId())
                .customerId(avroModel.getCustomerId())
                .documentId(avroModel.getDocumentId())
                .createdAt(avroModel.getCreatedAt())
                .documentNotificationStatus(DocumentNotificationStatus.valueOf(
                        avroModel.getDocumentNotificationStatus().name()))
                .recipientId(avroModel.getRecipientId())
                .recipientEmail(avroModel.getRecipientEmail())
                .subject(avroModel.getSubject())
                .message(avroModel.getMessage())
                .fileName(avroModel.getFileName())
                .contentType(avroModel.getContentType())
                .contentBase64(avroModel.getContentBase64())
                .failureMessages(avroModel.getFailureMessages() != null
                        ? new ArrayList<>(avroModel.getFailureMessages())
                        : new ArrayList<>())
                .build();
    }

    public NotificationResponseAvroModel documentEventPayloadToNotificationResponseAvroModel(String sagaId,
                                                                                              DocumentEventPayload payload) {
        return NotificationResponseAvroModel.newBuilder()
                .setId(payload.getNotificationId())
                .setSagaId(sagaId)
                .setNotificationId(payload.getNotificationId())
                .setDocumentId(payload.getDocumentId())
                .setRecipientId(payload.getRecipientId())
                .setCreatedAt(payload.getCreatedAt().toInstant())
                .setNotificationStatus(com.document.notification.system.kafka.document.avro.model.NotificationStatus
                        .valueOf(payload.getNotificationStatus()))
                .setFailureMessages(payload.getFailureMessages() != null
                        ? new ArrayList<>(payload.getFailureMessages())
                        : new ArrayList<>())
                .build();
    }
}
