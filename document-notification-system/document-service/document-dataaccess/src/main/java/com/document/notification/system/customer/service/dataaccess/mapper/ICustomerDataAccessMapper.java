package com.document.notification.system.customer.service.dataaccess.mapper;

import com.document.notification.system.customer.service.dataaccess.entity.CustomerEntity;
import com.document.notification.system.document.service.domain.entity.Customer;

public interface ICustomerDataAccessMapper {
    CustomerEntity customerToCustomerEntity(Customer customer);

    Customer customerEntityToCustomer(CustomerEntity customerEntity);


}
