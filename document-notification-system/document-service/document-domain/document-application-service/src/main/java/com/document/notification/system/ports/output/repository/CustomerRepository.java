package com.document.notification.system.ports.output.repository;

import com.document.notification.system.document.service.domain.entity.Customer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Optional<Customer> findCustomer(UUID customerId);
}