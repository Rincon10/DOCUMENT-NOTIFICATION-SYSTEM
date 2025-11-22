package com.document.notification.system.document.service.domain.valueobject;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Data
public class StreetAddress {
    private final UUID id;

    private final String postalCode;
    private final String street;
    private final String city;

    private final String country;

    @Builder
    public StreetAddress(UUID id, String postalCode, String street, String city, String country) {
        this.id = id;
        this.postalCode = postalCode;
        this.street = street;
        this.city = city;
        this.country = country;
    }
}
