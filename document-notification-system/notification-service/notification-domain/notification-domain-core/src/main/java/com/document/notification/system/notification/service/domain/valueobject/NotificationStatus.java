package com.document.notification.system.notification.service.domain.valueobject;

public enum NotificationStatus {
    NOTIFICATION_PENDING(false),
    NOTIFICATION_SENT(true),
    NOTIFICATION_CANCELLED(false),
    NOTIFICATION_FAILED(false);

    private final boolean successful;

    NotificationStatus(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }
}

