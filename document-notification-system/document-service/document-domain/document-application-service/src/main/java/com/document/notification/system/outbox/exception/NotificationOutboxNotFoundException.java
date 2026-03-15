package com.document.notification.system.outbox.exception;

public class NotificationOutboxNotFoundException extends RuntimeException {
    public NotificationOutboxNotFoundException(String message) {
        super(message);
    }
}
