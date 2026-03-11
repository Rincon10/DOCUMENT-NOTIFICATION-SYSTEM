package com.document.notification.system.notification.service.domain.entity;

import com.document.notification.system.domain.entity.AggregateRoot;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationId;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.notification.service.domain.valueobject.Recipient;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DocumentNotification extends AggregateRoot<NotificationId> {
    private final DocumentId documentId;
    private final CustomerId customerId;
    private final Recipient recipient;
    private final NotificationContent notificationContent;
    private final List<String> failureMessages;
    private NotificationStatus notificationStatus;

    public DocumentNotification(NotificationId id,
                                DocumentId documentId,
                                CustomerId customerId,
                                Recipient recipient,
                                NotificationContent notificationContent,
                                NotificationStatus notificationStatus) {
        setId(id);
        this.documentId = documentId;
        this.customerId = customerId;
        this.recipient = recipient;
        this.notificationContent = notificationContent;
        this.notificationStatus = notificationStatus;
        this.failureMessages = new ArrayList<>();
    }

    public void initializeNotification() {
        notificationStatus = NotificationStatus.NOTIFICATION_PENDING;
    }

    public void markAsSent() {
        notificationStatus = NotificationStatus.NOTIFICATION_SENT;
    }

    public void markAsFailed(List<String> failures) {
        notificationStatus = NotificationStatus.NOTIFICATION_FAILED;
        failureMessages.clear();
        if (failures != null) {
            failureMessages.addAll(failures);
        }
    }

    public void cancel() {
        notificationStatus = NotificationStatus.NOTIFICATION_CANCELLED;
    }
}

