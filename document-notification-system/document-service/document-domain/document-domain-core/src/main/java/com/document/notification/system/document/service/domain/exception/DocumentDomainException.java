package com.document.notification.system.document.service.domain.exception;

import com.document.notification.system.domain.exceptions.DomainException;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
public class DocumentDomainException extends DomainException {


    public DocumentDomainException(String message) {
        super(message);
    }

    public DocumentDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentDomainException(Throwable cause) {
        super(cause);
    }
}
