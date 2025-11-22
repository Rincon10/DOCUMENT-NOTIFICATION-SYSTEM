package com.document.notification.system.document.service.domain.entity;

import com.document.notification.system.document.service.domain.valueobject.DocumentItemId;
import com.document.notification.system.domain.entity.BaseEntity;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.Money;
import lombok.Builder;
import lombok.Data;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Data
public class DocumentItem extends BaseEntity<DocumentItemId> {
    private DocumentId documentId;

    private final Item item;
    private final int quantity;
    private final Money amount;
    private final Money subtotal;

    @Builder
    public DocumentItem(DocumentId documentId, Item item, int quantity, Money amount, Money subtotal) {
        this.documentId = documentId;
        this.item = item;
        this.quantity = quantity;
        this.amount = amount;
        this.subtotal = subtotal;
    }

    void initializeDocumentItem(DocumentId documentId, DocumentItemId documentItemId) {
        super.setId(documentItemId);
        this.documentId = documentId;
    }

    boolean isPriceValid() {
        return amount.isGreaterOrEqualToZero() &&
                amount.equals(item.getAmount()) &&
                amount.multiply(quantity).equals(subtotal);
    }
}
