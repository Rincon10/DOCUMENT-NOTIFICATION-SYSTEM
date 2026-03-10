package com.document.notification.system.dto.message;

import com.document.notification.system.domain.valueobject.DocumentNotificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/03/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    @NotNull
    private String id;
    @NotNull
    private String sagaId;
    @NotNull
    private String documentId;
    @NotNull
    private String recipentId;

    private Instant createdAt;
    private DocumentNotificationStatus documentNotificationStatus;
    private List<String> failureMessages;
}
