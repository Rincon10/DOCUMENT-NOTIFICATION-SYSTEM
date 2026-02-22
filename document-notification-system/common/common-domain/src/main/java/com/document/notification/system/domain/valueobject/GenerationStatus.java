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
    GENERATION_COMPLETED(true),

    /**
     * Document generation failed, compensation is initiated
     */
    GENERATION_FAILED(false),

    /**
     * Document generation cancelled by user
     */
    GENERATION_CANCELLED(false);

    private final boolean isSuccessful;

    GenerationStatus(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    /**
     * Returns whether this generation status represents a successful outcome.
     *
     * @return true if generation was successful, false otherwise
     */
    public boolean isSuccessful() {
        return isSuccessful;
    }
}

