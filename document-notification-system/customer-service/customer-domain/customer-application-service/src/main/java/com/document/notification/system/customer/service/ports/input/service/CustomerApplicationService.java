package com.document.notification.system.customer.service.ports.input.service;

import com.document.notification.system.customer.service.create.CreateCustomerCommand;
import com.document.notification.system.customer.service.create.CreateCustomerResponse;
import jakarta.validation.Valid;

public interface CustomerApplicationService {
    CreateCustomerResponse createCustomer(@Valid CreateCustomerCommand createCustomerCommand);
}
