package com.document.notification.system.document.service.domain.entity;

import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.domain.valueobject.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Getter
public class Document extends AggregateRoot<DocumentId> {

    public static final String FAILURE_MESSAGE_DELIMITER = ",";
    private final CustomerId customerId;
    private final StreetAddress deliveryAddress;
    private final DocumentType documentType;
    private final List<DocumentItem> documentItems;
    private final DocumentStatus documentStatus;
    private List<String> failureMessages;

    @Builder
    public Document(DocumentId documentId, CustomerId customerId, StreetAddress deliveryAddress, DocumentType documentType, List<DocumentItem> documentItems, DocumentStatus documentStatus) {
        setId(documentId);
        this.customerId = customerId;
        this.deliveryAddress = deliveryAddress;
        this.documentType = documentType;
        this.documentItems = documentItems;
        this.documentStatus = documentStatus;
    }

    public void validateDocument() {
    }

    public void initializeDocument() {
    }
}
