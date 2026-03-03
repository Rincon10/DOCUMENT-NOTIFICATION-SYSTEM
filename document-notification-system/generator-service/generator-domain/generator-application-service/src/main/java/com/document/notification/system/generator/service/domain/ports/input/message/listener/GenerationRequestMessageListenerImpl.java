package com.document.notification.system.generator.service.domain.ports.input.message.listener;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.helper.GenerationRequestHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Service
@AllArgsConstructor
public class GenerationRequestMessageListenerImpl implements GenerationRequestMessageListener {

    private final GenerationRequestHelper generationRequestHelper;

    @Override
    public void completedGeneration(GenerationRequest generationRequest) {
        generationRequestHelper.persistGenerationOnHistoryRecords(generationRequest);
    }

    @Override
    public void cancellGeneration(GenerationRequest generationRequest) {
        generationRequestHelper.persistCancelledGenerationOnHistoryRecords(generationRequest);

    }
}
