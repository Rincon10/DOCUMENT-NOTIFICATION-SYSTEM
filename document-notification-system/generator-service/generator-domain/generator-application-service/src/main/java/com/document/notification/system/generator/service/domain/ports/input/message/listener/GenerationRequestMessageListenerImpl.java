package com.document.notification.system.generator.service.domain.ports.input.message.listener;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Service
public class GenerationRequestMessageListenerImpl implements GenerationRequestMessageListener {
    @Override
    public void completedGeneration(GenerationRequest generationRequest) {
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @Override
    public void cancellGeneration(GenerationRequest generationRequest) {
        throw new UnsupportedOperationException("Method not implemented yet");

    }
}
