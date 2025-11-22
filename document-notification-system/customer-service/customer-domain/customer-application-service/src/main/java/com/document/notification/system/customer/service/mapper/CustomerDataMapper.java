package com.document.notification.system.customer.service.mapper;

import com.document.notification.system.customer.service.create.CreateCustomerCommand;
import com.document.notification.system.customer.service.create.CreateCustomerResponse;
import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.domain.valueobject.CustomerId;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Component
public class CustomerDataMapper {

    public Customer createCustomerCommandToCustomer(CreateCustomerCommand createCustomerCommand) {
        return Customer.builder()
                .customerId(new CustomerId(createCustomerCommand.getCustomerId()))
                .username(createCustomerCommand.getUsername())
                .firstName(createCustomerCommand.getFirstName())
                .lastName(createCustomerCommand.getLastName())
                .build();
    }

    public CreateCustomerResponse customerToCreateCustomerResponse(Customer customer, String message) {
        return CreateCustomerResponse.builder()
                .customerId(customer.getId().getValue())
                .message(message)
                .build();
    }
}
