package com.document.notification.system.ports.input.message.listener.generator;

import com.document.notification.system.dto.message.GenerationResponse;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
public interface GenerationResponseMessageListener {

    void generationCompleted(GenerationResponse generationResponse);

    void generationCancelled(GenerationResponse generationResponse);

}
