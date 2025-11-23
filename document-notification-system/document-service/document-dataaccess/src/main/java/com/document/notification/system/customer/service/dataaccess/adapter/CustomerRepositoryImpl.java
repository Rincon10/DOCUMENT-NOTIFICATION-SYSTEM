package com.document.notification.system.customer.service.dataaccess.adapter;

import com.document.notification.system.customer.service.dataaccess.entity.CustomerEntity;
import com.document.notification.system.customer.service.dataaccess.mapper.ICustomerDataAccessMapper;
import com.document.notification.system.customer.service.dataaccess.repository.CustomerJpaRepository;
import com.document.notification.system.document.service.domain.entity.Customer;
import com.document.notification.system.ports.output.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Component
public class CustomerRepositoryImpl implements CustomerRepository {

    private final ICustomerDataAccessMapper customerDataAccessMapper;
    private final CustomerJpaRepository customerJpaRepository;

    public CustomerRepositoryImpl(ICustomerDataAccessMapper customerDataAccessMapper, CustomerJpaRepository customerJpaRepository) {
        this.customerDataAccessMapper = customerDataAccessMapper;
        this.customerJpaRepository = customerJpaRepository;
    }

    @Override
    public Optional<Customer> findCustomer(UUID customerId) {
        Optional<CustomerEntity> optionalCustomerEntity = customerJpaRepository.findById(customerId);
        Optional<Customer> optionalCustomer = optionalCustomerEntity.map(customerEntity -> customerDataAccessMapper.customerEntityToCustomer(customerEntity));
        return optionalCustomer;
    }
}
