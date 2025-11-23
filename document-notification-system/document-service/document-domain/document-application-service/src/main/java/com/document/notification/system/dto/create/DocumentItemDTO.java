package com.document.notification.system.dto.create;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentItemDTO {
    @NotNull
    private final UUID itemId;

    private final BigDecimal amount;
    
    private BigDecimal lateInterest;

    private BigDecimal regularInterest;

    private BigDecimal subTotal;
}
