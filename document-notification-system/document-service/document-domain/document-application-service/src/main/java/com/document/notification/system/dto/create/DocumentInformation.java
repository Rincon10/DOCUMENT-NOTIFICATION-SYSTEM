package com.document.notification.system.dto.create;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@Builder
@AllArgsConstructor
public class DocumentInformation {

    @NotNull
    private LocalDate periodStartDate;

    @NotNull
    private LocalDate periodEndDate;

    private BigDecimal totalLateInterest;

    private BigDecimal totalRegularInterest;

    private BigDecimal totalAmount;

    @NotNull
    private final DocumentAddress address;
}
