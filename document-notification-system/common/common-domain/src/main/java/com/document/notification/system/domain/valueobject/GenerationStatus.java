package com.document.notification.system.domain.valueobject;

/**
 * Generation SAGA status enum.
 * Represents the state of the SAGA orchestration between document service and generator service.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 18/02/2026
 */
public enum GenerationStatus {


    /**
     * Document generation completed successfully
     */
    GENERATION_COMPLETED,

    /**
     * Document generation failed, compensation is initiated
     */
    GENERATION_FAILED,


    /**
     * Document generation cancelled by user
     */
    GENERATION_CANCELLED
}

