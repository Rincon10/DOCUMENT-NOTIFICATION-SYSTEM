package com.document.notification.system.outbox.exception;

public class GenerationOutboxNotFoundException extends RuntimeException {
    public GenerationOutboxNotFoundException(String message) {
        super(message);
    }
}

