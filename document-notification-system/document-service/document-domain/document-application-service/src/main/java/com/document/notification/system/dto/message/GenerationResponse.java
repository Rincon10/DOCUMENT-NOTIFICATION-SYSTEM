package com.document.notification.system.dto.message;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Getter
@Builder
@AllArgsConstructor
public class GenerationResponse {
    @NotNull
    private final UUID id;
    @NotNull
    private final UUID sagaId;
    @NotNull
    private final UUID generatorId;
    @NotNull
    private final UUID customerId;
    @NotNull
    private final UUID documentId;
    @NotNull
    private final Instant createdAt;
    @NotNull
    private final GenerationStatus generationStatus;
    @NotNull
    private final List<String> failureMessages;
}
