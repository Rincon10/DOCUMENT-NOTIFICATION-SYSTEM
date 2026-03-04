package com.document.notification.system.generator.service.dataaccess.entity;


import com.document.notification.system.domain.valueobject.GenerationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_generation")
@Entity
public class DocumentGenerationEntity {
    @Id
    private UUID id;
    private UUID customerId;
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    private GenerationStatus status;
    private ZonedDateTime createdAt;


}
