package com.document.notification.system.mapper;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.entity.DocumentItem;
import com.document.notification.system.document.service.domain.entity.Item;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentGenerationStatus;
import com.document.notification.system.domain.valueobject.Money;
import com.document.notification.system.dto.create.*;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Component
public class DocumentDataMapper implements IDocumentDataMapper {

    private Item item(DocumentItemDTO label) {
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
                .id(UUID.randomUUID())
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
                .periodStartDate(documentInformation.getPeriodStartDate())
                .periodEndDate(documentInformation.getPeriodEndDate())
                .build();
    }


    @Override
    public CreateDocumentResponse documentToCreateDocumentResponse(Document document, String message) {
        return CreateDocumentResponse.builder()
                .accountId(document.getCustomerId().getValue())
                .documentStatus(document.getDocumentStatus())
                .message(message)
                .build();

    }

    @Override
    public DocumentGenerationEventPayload documentCreatedEventToDocumentGenerationEventPayload(DocumentCreatedEvent documentCreatedEvent) {
        Document document = documentCreatedEvent.getDocument();
        return DocumentGenerationEventPayload.builder()
                .documentId(documentCreatedEvent.getDocument().getId().getValue().toString())
                .customerId(documentCreatedEvent.getDocument().getCustomerId().getValue().toString())
                .createdAt(documentCreatedEvent.getCreatedAt())
                .documentGenerationStatus(DocumentGenerationStatus.PENDING.name())
                .documentType(document.getDocumentType().name())
                .fileName(buildFileName(document))
                .periodStartDate(document.getPeriodStartDate())
                .periodEndDate(document.getPeriodEndDate())
                .deliveryAddress(buildDeliveryAddress(document.getDeliveryAddress()))
                .documentStatus(document.getDocumentStatus().name())
                .itemCount(document.getDocumentItems() != null ? document.getDocumentItems().size() : 0)
                .metadata(buildMetadata(document))
                .fileName(buildFileName(document))
                .build();
    }

    private String buildFileName(Document document) {
        return "document-" + document.getId().getValue() + "." + document.getDocumentType().name().toLowerCase();
    }

    private String buildDeliveryAddress(StreetAddress address) {
        if (address == null) {
            return null;
        }
        return String.join(", ",
                List.of(address.getStreet(), address.getCity(), address.getState(), address.getZipCode(), address.getCountry())
                        .stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList());
    }

    private Map<String, String> buildMetadata(Document document) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "document-service");
        metadata.put("documentId", document.getId().getValue().toString());
        metadata.put("customerId", document.getCustomerId().getValue().toString());
        metadata.put("documentType", document.getDocumentType().name());
        if (document.getPeriodStartDate() != null) {
            metadata.put("periodStartDate", document.getPeriodStartDate().toString());
        }
        if (document.getPeriodEndDate() != null) {
            metadata.put("periodEndDate", document.getPeriodEndDate().toString());
        }
        return metadata;
    }

    @Override
    public DocumentNotificationEventPayload documentCreatedEventToDocumentNotificationEventPayload(DocumentCreatedEvent documentCreatedEvent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
