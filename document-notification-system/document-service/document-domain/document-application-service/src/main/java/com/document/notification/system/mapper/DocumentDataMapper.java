package com.document.notification.system.mapper;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.entity.DocumentItem;
import com.document.notification.system.document.service.domain.entity.Item;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.Money;
import com.document.notification.system.dto.create.*;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Component
public class DocumentDataMapper implements IDocumentDataMapper {

    private Item item(DocumentItemDTO label){
        return Item.builder()
                .name(label.getItemId().toString())
                .amount(new Money(label.getAmount()))
                .build();
    }
    private List<DocumentItem> documentItemDTOtoDocumentItemList(List<DocumentItemDTO> labels) {
        return labels.stream()
                .map(label -> DocumentItem.builder()
                        .item(item(label))
                        .lateInterest(new Money(label.getLateInterest()))
                        .regularInterest(new Money(label.getRegularInterest()))
                        .build())
                .toList();
    }
    private StreetAddress documentAddressToStreetAddress(DocumentAddressDTO documentAddress) {
        return StreetAddress.builder()
                .id( UUID.randomUUID())
                .street(documentAddress.getStreet())
                .city(documentAddress.getCity())
                .state(documentAddress.getState())
                .zipCode(documentAddress.getZipCode())
                .country(documentAddress.getCountry())
                .build();


    }

    @Override
    public Document createDocumentCommandToDocument(CreateDocumentCommand createDocumentCommand) {
        final DocumentInformationDTO documentInformation = createDocumentCommand.getDocumentInformation();
        return Document.builder()
                .customerId(new CustomerId(createDocumentCommand.getCustomerId()))
                .deliveryAddress(documentAddressToStreetAddress(documentInformation.getAddress()))
                .documentType(documentInformation.getDocumentType())
                .documentItems(documentItemDTOtoDocumentItemList(createDocumentCommand.getLabels()))
                .build();
    }


    @Override
    public CreateDocumentResponse documentToCreateDocumentResponse(Document document, String message) {
        throw new UnsupportedOperationException("Not implemented yet");

    }

    @Override
    public DocumentGenerationOutboxMessage documentCreatedEventToDocumentGenerationEventPayload(DocumentCreatedEvent documentCreatedEvent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
