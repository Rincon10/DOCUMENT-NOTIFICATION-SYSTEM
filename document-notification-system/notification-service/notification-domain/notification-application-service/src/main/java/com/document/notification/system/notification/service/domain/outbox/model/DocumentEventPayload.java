package com.document.notification.system.notification.service.domain.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DocumentEventPayload {
    @JsonProperty
    private String notificationId;

    @JsonProperty
    private String customerId;

    @JsonProperty
    private String documentId;

    @JsonProperty
    private String recipientId;

    @JsonProperty
    private ZonedDateTime createdAt;

    @JsonProperty
    private String notificationStatus;

    @JsonProperty
    private List<String> failureMessages;
}
