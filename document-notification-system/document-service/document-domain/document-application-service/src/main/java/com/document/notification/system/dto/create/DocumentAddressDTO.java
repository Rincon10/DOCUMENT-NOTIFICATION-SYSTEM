package com.document.notification.system.dto.create;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */

@Getter
@Builder
@AllArgsConstructor
public class DocumentAddressDTO {
    @NotNull
    @Max(value = 20)
    private final String postalCode;

    @NotNull
    @Max(value = 200)
    private final String street;

    @NotNull
    @Max(value = 50)
    private final String city;
    @NotNull
    @Max(value = 50)
    private final String state;
    @NotNull
    @Max(value = 50)
    private final String zipCode;

    @NotNull
    @Max(value = 50)
    private final String country;
}
