package com.document.notification.system.document.service.dataaccess.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(DocumentItemEntityId.class)
@Table(name = "document_items")
@Entity
public class DocumentItemEntity {
    @Id
    private Long id;
    @Id
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "DOCUMENT_ID")
    private DocumentEntity document;

    private UUID itemId;
    private BigDecimal value;
    private Integer quantity;
    private BigDecimal subTotal;


}
