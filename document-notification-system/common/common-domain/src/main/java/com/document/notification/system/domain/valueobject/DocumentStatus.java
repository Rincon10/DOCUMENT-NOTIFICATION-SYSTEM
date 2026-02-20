package com.document.notification.system.domain.valueobject;

/**
 * Document lifecycle status enum.
 * Represents the current state of a document in the system.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
public enum DocumentStatus {
    /**
     * Document created and waiting for generation
     */
    PENDING,

    /**
     * Document file has been generated successfully
     */
    GENERATED,

    /**
     * Document has been sent to the customer
     */
    SENT,

    /**
     * Document cancellation in progress
     */
    CANCELLING,

    /**
     * Document has been cancelled
     */
    CANCELLED
}
