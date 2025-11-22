package com.document.notification.system.customer.service.ports.output.repository;

import com.document.notification.system.customer.service.entity.Customer;

public interface CustomerRepository {
    Customer createCustomer(Customer customer);
}
