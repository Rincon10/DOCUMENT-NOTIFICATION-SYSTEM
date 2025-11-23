package com.document.notification.system.customer.service.exception;

import com.document.notification.system.domain.exceptions.DomainException;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
public class CustomerDomainException extends DomainException {

    public CustomerDomainException(String message) {
        super(message);
    }
}