package com.document.notification.system.customer.service.dataaccess.customer.adapter;

import com.document.notification.system.customer.service.dataaccess.customer.mapper.CustomerDataAccessMapper;
import com.document.notification.system.customer.service.dataaccess.customer.repository.CustomerJpaRepository;
import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.customer.service.ports.output.repository.CustomerRepository;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Component
public class CustomerRepositoryImpl implements CustomerRepository {
    private final CustomerDataAccessMapper customerDataAccessMapper;
    private final CustomerJpaRepository customerJpaRepository;

    public CustomerRepositoryImpl(CustomerDataAccessMapper customerDataAccessMapper, CustomerJpaRepository customerJpaRepository) {
        this.customerDataAccessMapper = customerDataAccessMapper;
        this.customerJpaRepository = customerJpaRepository;
    }

    @Override
    public Customer createCustomer(Customer customer) {
        throw new UnsupportedOperationException("Not implemented yet");

    }
}
