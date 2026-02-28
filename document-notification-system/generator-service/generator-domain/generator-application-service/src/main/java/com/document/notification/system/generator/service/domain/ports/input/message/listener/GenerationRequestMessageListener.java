package com.document.notification.system.generator.service.domain.ports.input.message.listener;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;

public interface GenerationRequestMessageListener {
    void completedGeneration(GenerationRequest generationRequest);

    void cancellGeneration(GenerationRequest generationRequest);
}
