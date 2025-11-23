package com.document.notification.system.customer.service;

import com.document.notification.system.customer.service.entity.Customer;
import com.document.notification.system.customer.service.event.CustomerCreatedEvent;
import com.document.notification.system.domain.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Slf4j
public class CustomerDomainServiceImpl implements CustomerDomainService {
    @Override
    public CustomerCreatedEvent validateAndInitiateCustomer(Customer customer) {
        // Any Business logic required to run
        log.info("Customer with id: {} is initiated", customer.getId().getValue());
        return new CustomerCreatedEvent(customer, DateUtils.getZoneDateTimeByUTCZoneId());
    }
}
