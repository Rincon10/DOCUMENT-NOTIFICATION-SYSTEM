package com.document.notification.system.dto.create;

import com.document.notification.system.document.service.domain.entity.DocumentItem;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@Builder
@AllArgsConstructor
public class CreateDocumentCommand {
    @NotNull
    private final UUID customerId;
    @NotNull
    private final List<DocumentItem> labels;
    @NotNull
    private final DocumentInformation documentInformation;

}
