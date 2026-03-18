package com.document.notification.system.outbox.model.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentNotificationEventPayload {
    @JsonProperty
    private String documentId;

    @JsonProperty
    private String customerId;

    @JsonProperty
    private ZonedDateTime createdAt;

    @JsonProperty
    private String documentNotificationStatus;

    @JsonProperty
    private String recipientId;

    @JsonProperty
    private String subject;

    @JsonProperty
    private String message;

    @JsonProperty
    private String fileName;

    @JsonProperty
    private String contentType;

    @JsonProperty
    private String documentType;

    @JsonProperty
    private String contentBase64;

    @JsonProperty
    private List<String> failureMessages;
}
