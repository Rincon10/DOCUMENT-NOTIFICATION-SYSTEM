package com.document.notification.system.document.entity;

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
    @Id
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "DOCUMENT_ID")
    public DocumentEntity document;

    private String postalCode;
    private String addressLine;
    private String city;
    private String country;

}
