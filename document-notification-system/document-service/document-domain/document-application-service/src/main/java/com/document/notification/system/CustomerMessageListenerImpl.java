package com.document.notification.system;

import com.document.notification.system.dto.message.CustomerModel;
import com.document.notification.system.ports.input.message.listener.customer.ICustomerMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Slf4j
@Service
public class CustomerMessageListenerImpl implements ICustomerMessageListener {
    @Override
    public void customerCreated(CustomerModel customerModel) {
        throw new UnsupportedOperationException("Method not implemented yet");
    }
}
