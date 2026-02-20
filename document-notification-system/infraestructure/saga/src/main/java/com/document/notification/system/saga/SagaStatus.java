package com.document.notification.system.saga;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
public enum SagaStatus {
    PROCESSING, COMPENSATING, STARTED, SUCCESSFUL, FAILED, COMPENSATED
}
