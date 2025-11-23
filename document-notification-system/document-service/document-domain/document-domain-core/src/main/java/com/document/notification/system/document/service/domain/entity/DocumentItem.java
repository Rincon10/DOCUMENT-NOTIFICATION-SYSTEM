package com.document.notification.system.document.service.domain.entity;

import com.document.notification.system.document.service.domain.valueobject.DocumentItemId;
import com.document.notification.system.domain.entity.BaseEntity;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.Money;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Data
public class DocumentItem extends BaseEntity<DocumentItemId> {
    private final Item item;
    private DocumentId documentId;
    private Money lateInterest;
    private Money regularInterest;
    private Money subTotal;

    @Builder
    public DocumentItem(DocumentId documentId, Item item, Money lateInterest, Money regularInterest, Money subTotal) {
        this.documentId = documentId;
        this.item = item;
        this.lateInterest = lateInterest;
        this.regularInterest = regularInterest;
        this.subTotal = subTotal;
    }

    void initializeDocumentItem(DocumentId documentId, DocumentItemId documentItemId) {
        super.setId(documentItemId);
        this.documentId = documentId;
    }

    boolean isValidInterest() {
        return regularInterest.isGreaterOrEqualToZero() && lateInterest.isGreaterOrEqualToZero();
    }
}
