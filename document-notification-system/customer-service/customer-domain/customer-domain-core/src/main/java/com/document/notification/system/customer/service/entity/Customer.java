package com.document.notification.system.customer.service.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@AllArgsConstructor
@Getter
@Setter
public class Customer  extends AggregateRoot<CustomerId> {
    private final String username;
    private final String firstName;
    private final String lastName;
}
