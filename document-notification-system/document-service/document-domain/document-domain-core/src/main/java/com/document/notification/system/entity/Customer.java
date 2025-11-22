package com.document.notification.system.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import lombok.*;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@AllArgsConstructor
@Getter
@Setter
public class Customer extends AggregateRoot<CustomerId> {
    private String username;
    private String firstName;
    private String lastName;

    @Builder
    public Customer(CustomerId customerId, String username, String firstName, String lastName) {
        super.setId(customerId);
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

}
