package com.document.notification.system.outbox.model.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentGenerationEventPayload {
    @JsonProperty
    private String documentId;
    @JsonProperty
    private String customerId;
    @JsonProperty
    private ZonedDateTime createdAt;


}
