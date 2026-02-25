package com.document.notification.system.document.service.dataaccess.mapper;

import com.document.notification.system.document.service.dataaccess.entity.DocumentAddressEntity;
import com.document.notification.system.document.service.dataaccess.entity.DocumentEntity;
import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.entity.DocumentItem;
import com.document.notification.system.document.service.domain.valueobject.StreetAddress;
import com.document.notification.system.domain.constants.GlobalConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Component
public class DocumentDataAccessMapperImpl implements DocumentDataAccessMapperI {

    public BigDecimal totalRegularInterest(List<DocumentItem> items) {
        return items.stream()
                .map(item -> item.getRegularInterest().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalLateInterest(List<DocumentItem> items) {
        return items.stream()
                .map(item -> item.getLateInterest().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Override
    public DocumentEntity documentToDocumentEntity(Document document) {

        String failuresMessages = document.getFailureMessages() != null ? String.join(GlobalConstants.FAILURE_MESSAGE_DELIMITER, document.getFailureMessages()) : "";

        List<DocumentItem> items = Optional.ofNullable(document.getDocumentItems())
                .orElse(Collections.emptyList());

        BigDecimal totalRegularInterest = totalRegularInterest(items);
        BigDecimal totalLateInterest = totalLateInterest(items);

        return DocumentEntity.builder()
                .id(document.getId().getValue())
                .customerId(document.getCustomerId().getValue())
                .address(streetAddressToAddress(document.getDeliveryAddress()))
                .documentStatus(document.getDocumentStatus())
                .totalLateInterest(totalLateInterest)
                .totalRegularInterest(totalLateInterest)
                .periodStartDate(document.getPeriodStartDate())
                .periodEndDate(document.getPeriodEndDate())
                .totalAmount(totalRegularInterest.add(totalLateInterest))
                .failureMessages(failuresMessages)
                .build();
    }

    @Override
    public Document documentEntityToDocument(DocumentEntity documentEntity) {
        throw new RuntimeException("Not implemntede yet");
    }

    private DocumentAddressEntity streetAddressToAddress(StreetAddress streetAddress) {
        if (streetAddress == null) {
            return null;
        }
        return DocumentAddressEntity.builder()
                .id(streetAddress.getId())
                .addressLine(streetAddress.getStreet())
                .city(streetAddress.getCity())
                .state(streetAddress.getState())
                .postalCode(streetAddress.getZipCode())
                .build();
    }
}
