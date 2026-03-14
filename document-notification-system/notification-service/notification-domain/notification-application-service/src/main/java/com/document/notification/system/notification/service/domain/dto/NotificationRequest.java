package com.document.notification.system.notification.service.domain.dto;

import com.document.notification.system.domain.valueobject.DocumentNotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class NotificationRequest {
    private String id;
    private String sagaId;
    private String customerId;
    private String documentId;
    private Instant createdAt;
    private DocumentNotificationStatus documentNotificationStatus;
    private String recipientId;
    private String subject;
    private String message;
    private String fileName;
    private String contentType;
    private String contentBase64;
    private List<String> failureMessages;
}
