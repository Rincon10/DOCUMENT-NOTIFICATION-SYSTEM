package com.document.notification.system.notification.service.domain.valueobject;

import lombok.Builder;
import lombok.Getter;

/**
 * Typed data required to send a notification.
 * Analogous to GenerationContentData in generator-service.
 * Contains contextual information for the notification delivery.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Getter
@Builder(toBuilder = true)
public class NotificationData {
    private final String notificationId;
    private final String documentId;
    private final String customerId;
    private final String requestId;
    private final String sagaId;
}
