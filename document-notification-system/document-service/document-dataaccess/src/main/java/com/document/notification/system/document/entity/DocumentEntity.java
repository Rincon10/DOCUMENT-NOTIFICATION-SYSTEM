package com.document.notification.system.document.entity;


import com.document.notification.system.domain.valueobject.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "documents")
@Entity
public class DocumentEntity {
    @Id
    private UUID id;

    private UUID customerId;

    private UUID accountId;

    private String fileName;

    private String filePath;

    @Column(nullable = false)
    private LocalDate periodStartDate;

    @Column(nullable = false)
    private LocalDate periodEndDate;

    private BigDecimal totalLateInterest;

    private BigDecimal totalRegularInterest;

    private BigDecimal totalAmount;

    private String createdBy;

    @Column( nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;

    private String failureMessages;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL)
    private DocumentAddressEntity address;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentItemEntity> items;


}
