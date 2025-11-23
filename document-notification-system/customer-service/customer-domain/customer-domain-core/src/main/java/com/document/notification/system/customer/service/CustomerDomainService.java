package com.document.notification.system.customer.service;

import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.customer.service.event.CustomerCreatedEvent;

public interface CustomerDomainService {

    CustomerCreatedEvent validateAndInitiateCustomer(Customer customer);
}
