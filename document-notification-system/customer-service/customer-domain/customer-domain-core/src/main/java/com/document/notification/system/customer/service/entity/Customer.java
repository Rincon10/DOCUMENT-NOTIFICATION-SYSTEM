package com.document.notification.system.customer.service.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */

@Getter
@Setter
public class Customer  extends AggregateRoot<CustomerId> {
    private final String username;
    private final String firstName;
    private final String lastName;


    @Builder
    public Customer(CustomerId customerId,String username, String firstName, String lastName) {
        setId(customerId);
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
