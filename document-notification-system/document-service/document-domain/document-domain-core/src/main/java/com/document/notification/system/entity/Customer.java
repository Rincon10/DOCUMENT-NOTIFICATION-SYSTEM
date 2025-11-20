package com.document.notification.system.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
public class Customer extends AggregateRoot<CustomerId> {
    public Customer(CustomerId customerId) {
        this.setId(customerId);
    }

    public Customer() {}
}
