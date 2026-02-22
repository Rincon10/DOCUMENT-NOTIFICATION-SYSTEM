package com.document.notification.system.ports.input.message.listener.customer;

import com.document.notification.system.dto.message.CustomerModel;

public interface ICustomerMessageListener {
    void customerCreated(CustomerModel customerModel);
}
