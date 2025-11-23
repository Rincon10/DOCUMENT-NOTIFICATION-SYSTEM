package com.document.notification.system.customer.service.dataaccess.mapper;

import com.document.notification.system.customer.service.dataaccess.entity.CustomerEntity;
import com.document.notification.system.document.service.domain.entity.Customer;
import com.document.notification.system.domain.valueobject.CustomerId;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Component
public class CustomerDataAccessMapperImpl implements ICustomerDataAccessMapper {
    @Override
    public CustomerEntity customerToCustomerEntity(Customer customer) {
        return CustomerEntity.builder()
                .id(customer.getId().getValue())
                .username(customer.getUsername())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }

    @Override
    public Customer customerEntityToCustomer(CustomerEntity customerEntity) {
        return Customer.builder()
                .customerId(new CustomerId(customerEntity.getId()))
                .username(customerEntity.getUsername())
                .firstName(customerEntity.getFirstName())
                .lastName(customerEntity.getLastName())
                .build();
    }
}
