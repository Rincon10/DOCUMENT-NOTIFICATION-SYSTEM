package com.document.notification.system.customer.service.dataaccess.customer.mapper;

import com.document.notification.system.customer.service.dataaccess.customer.entity.CustomerEntity;
import com.document.notification.system.customer.service.entity.Customer;

public interface CustomerDataAccessMapper {
    public Customer customerEntityToCustomer(CustomerEntity customerEntity);

    public CustomerEntity customerToCustomerEntity(Customer customer);
}
