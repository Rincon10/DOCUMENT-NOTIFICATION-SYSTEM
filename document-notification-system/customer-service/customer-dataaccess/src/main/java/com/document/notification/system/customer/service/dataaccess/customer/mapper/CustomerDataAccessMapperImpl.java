package com.document.notification.system.customer.service.dataaccess.customer.mapper;

import com.document.notification.system.customer.service.dataaccess.customer.entity.CustomerEntity;
import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.domain.valueobject.CustomerId;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Component
public class CustomerDataAccessMapperImpl implements CustomerDataAccessMapper {
    @Override
    public Customer customerEntityToCustomer(CustomerEntity customerEntity) {
        CustomerId customerId = new CustomerId(customerEntity.getId());
        return Customer.builder()
                .customerId(customerId)
                .username(customerEntity.getUsername())
                .firstName(customerEntity.getFirstName())
                .lastName(customerEntity.getLastName())
                .build();
    }

    @Override
    public CustomerEntity customerToCustomerEntity(Customer customer) {
        return CustomerEntity.builder()
                .id(customer.getId().getValue())
                .username(customer.getUsername())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }
}
