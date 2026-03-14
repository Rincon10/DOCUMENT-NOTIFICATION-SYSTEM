package com.document.notification.system.customer.service.ports.output.message.publisher.mapper;

import com.document.notification.system.customer.service.event.CustomerCreatedEvent;
import com.document.notification.system.kafka.document.avro.model.CustomerAvroModel;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Component
public class CustomerMessagingDataMapper {
    public CustomerAvroModel paymentResponseAvroModelToPaymentResponse(CustomerCreatedEvent
                                                                               customerCreatedEvent) {
        return CustomerAvroModel.newBuilder()
                .setId(customerCreatedEvent.getCustomer().getId().getValue().toString())
                .setFirstName(customerCreatedEvent.getCustomer().getFirstName())
                .setLastName(customerCreatedEvent.getCustomer().getLastName())
                .setUsername(customerCreatedEvent.getCustomer().getUsername())
                .build();

    }
}