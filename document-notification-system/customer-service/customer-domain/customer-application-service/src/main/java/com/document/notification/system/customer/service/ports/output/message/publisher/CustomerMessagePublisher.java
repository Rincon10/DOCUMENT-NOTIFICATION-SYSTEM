package com.document.notification.system.customer.service.ports.output.message.publisher;

import com.document.notification.system.customer.service.event.CustomerCreatedEvent;

public interface CustomerMessagePublisher {

    void publish(CustomerCreatedEvent customerCreatedEvent);
}
