package com.document.notification.system.ports.output.repository;

import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.entity.Customer;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findCustomer(CustomerId customerId);
}