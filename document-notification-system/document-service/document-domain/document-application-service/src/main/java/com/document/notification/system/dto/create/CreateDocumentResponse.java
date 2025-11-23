package com.document.notification.system.dto.create;

import com.document.notification.system.domain.valueobject.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@Builder
@AllArgsConstructor
public class CreateDocumentResponse {
    @NotNull
    private UUID accountId;
    @NotNull
    private final DocumentStatus documentStatus;
    @NotNull
    private final String message;
}
