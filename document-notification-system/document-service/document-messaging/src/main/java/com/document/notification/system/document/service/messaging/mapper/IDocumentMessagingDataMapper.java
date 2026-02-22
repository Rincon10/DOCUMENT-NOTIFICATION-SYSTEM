package com.document.notification.system.document.service.messaging.mapper;

import com.document.notification.system.dto.message.CustomerModel;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.kafka.document.avro.model.CustomerAvroModel;
import com.document.notification.system.kafka.document.avro.model.GeneratorResponseAvroModel;

public interface IDocumentMessagingDataMapper {

    CustomerModel customerAvroModeltoCustomerModel(CustomerAvroModel customerAvroModel);

    GenerationResponse generatorResponseAvroModelToGenerationResponse(GeneratorResponseAvroModel generatorResponseAvroModel);
}
