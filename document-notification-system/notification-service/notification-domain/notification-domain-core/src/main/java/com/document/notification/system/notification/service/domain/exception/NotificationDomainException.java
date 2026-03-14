package com.document.notification.system.notification.service.domain.exception;

public class NotificationDomainException extends RuntimeException {
    public NotificationDomainException(String message) {
        super(message);
    }

    public NotificationDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
