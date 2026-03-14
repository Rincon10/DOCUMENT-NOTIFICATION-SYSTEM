package com.document.notification.system.notification.service.domain.dto;

import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String sagaId;
    private String documentId;
    private String recipientId;
    private ZonedDateTime createdAt;
    private NotificationStatus notificationStatus;
    private List<String> failureMessages;
}
