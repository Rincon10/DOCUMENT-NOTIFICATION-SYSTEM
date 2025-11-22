package com.document.notification.system.document.service.domain.entity;

import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.domain.valueobject.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@AllArgsConstructor
@Getter
public class Document extends AggregateRoot<DocumentId> {

    private final CustomerId customerId;
    private final StreetAddress deliveryAddress;
    private final DocumentType documentType;

    private final List<DocumentItem> documentItems;
    private final DocumentStatus documentStatus;


    private List<String> failureMessages;
    public static final String FAILURE_MESSAGE_DELIMITER = ",";



}
