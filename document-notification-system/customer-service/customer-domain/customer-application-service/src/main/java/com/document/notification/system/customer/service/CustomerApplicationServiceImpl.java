package com.document.notification.system.customer.service;

import com.document.notification.system.customer.service.create.CreateCustomerCommand;
import com.document.notification.system.customer.service.create.CreateCustomerResponse;
import com.document.notification.system.customer.service.event.CustomerCreatedEvent;
import com.document.notification.system.customer.service.mapper.CustomerDataMapper;
import com.document.notification.system.customer.service.ports.input.service.CustomerApplicationService;
import com.document.notification.system.domain.constants.ResponseConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Slf4j
@Validated
@Service
public class CustomerApplicationServiceImpl implements CustomerApplicationService {

    private final CustomerCreateCommandHandler customerCreateCommandHandler;

    private final CustomerDataMapper customerDataMapper;

    public CustomerApplicationServiceImpl(CustomerCreateCommandHandler customerCreateCommandHandler, CustomerDataMapper customerDataMapper) {
        this.customerCreateCommandHandler = customerCreateCommandHandler;
        this.customerDataMapper = customerDataMapper;
    }


    @Override
    public CreateCustomerResponse createCustomer(CreateCustomerCommand createCustomerCommand) {
        CustomerCreatedEvent customerCreatedEvent = customerCreateCommandHandler.createCustomer(createCustomerCommand);

        CreateCustomerResponse createCustomerResponse = customerDataMapper.customerToCreateCustomerResponse(customerCreatedEvent.getCustomer(), ResponseConstants.CUSTOMER_CREATED_SUCCESSFULLY);
        return createCustomerResponse;
    }
}
