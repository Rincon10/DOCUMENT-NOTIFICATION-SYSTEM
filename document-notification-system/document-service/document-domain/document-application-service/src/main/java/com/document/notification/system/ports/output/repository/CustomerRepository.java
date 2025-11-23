package com.document.notification.system.ports.output.repository;

import com.document.notification.system.document.service.domain.entity.Customer;
import com.document.notification.system.domain.valueobject.CustomerId;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findCustomer(CustomerId customerId);
}