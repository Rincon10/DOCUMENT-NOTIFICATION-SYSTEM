package com.document.notification.system.customer.service.event;

import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.domain.events.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@AllArgsConstructor
@Getter
public class CustomerCreatedEvent implements DomainEvent<Customer> {

    private final Customer customer;

    private final ZonedDateTime createdAt;

}
