package com.document.notification.system.mapper;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;

import com.document.notification.system.dto.create.DocumentAddressDTO;

import com.document.notification.system.dto.create.DocumentInformationDTO;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Component
public class DocumentDataMapper implements IDocumentDataMapper {

    private StreetAddress documentAddressToStreetAddress(DocumentAddressDTO documentAddress) {
        return StreetAddress.builder()
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
                .documentItems(null)
                .build();
    }

    @Override
    public CreateDocumentResponse documentToCreateDocumentResponse(Document document, String message) {
        return null;
    }
}
