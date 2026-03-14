package com.document.notification.system.notification.service.dataaccess.entity;

import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_notification")
@Entity
public class DocumentNotificationEntity {
    @Id
    private UUID id;
    private UUID customerId;
    private UUID documentId;
    private String recipientId;
    private String subject;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private ZonedDateTime createdAt;
}
