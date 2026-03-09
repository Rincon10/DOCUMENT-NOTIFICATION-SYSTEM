package com.document.notification.system.outbox.model.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

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
    @JsonProperty
    private String documentGenerationStatus;
    @JsonProperty
    private String documentType;
    @JsonProperty
    private String fileName;
    @JsonProperty
    private LocalDate periodStartDate;
    @JsonProperty
    private LocalDate periodEndDate;
    @JsonProperty
    private String deliveryAddress;
    @JsonProperty
    private String documentStatus;
    @JsonProperty
    private Integer itemCount;
    @JsonProperty
    private Map<String, String> metadata;


}
