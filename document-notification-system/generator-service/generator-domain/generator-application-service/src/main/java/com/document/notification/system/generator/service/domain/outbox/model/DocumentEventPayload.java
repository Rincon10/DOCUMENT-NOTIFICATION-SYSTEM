package com.document.notification.system.generator.service.domain.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentEventPayload {
    @JsonProperty
    private String generationId;

    @JsonProperty
    private String customerId;

    @JsonProperty
    private String documentId;

    @JsonProperty
    private ZonedDateTime createdAt;

    @JsonProperty
    private String generationStatus;

    @JsonProperty
    private List<String> failureMessages;

}
