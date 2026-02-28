package com.document.notification.system.document.service.dataaccess.entity;

import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "document_address")
@Entity
public class DocumentAddressEntity {
    @OneToOne(optional = true)
    @JoinColumn(name = "DOCUMENT_ID", nullable = true)
    public DocumentEntity document;
    @Id
    private UUID id;
    private String state;
    private String postalCode;
    private String addressLine;
    private String city;
    private String country;

}
