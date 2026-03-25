package com.document.notification.system.messages;

import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.ports.input.message.listener.generator.GenerationResponseMessageListener;
import com.document.notification.system.saga.DocumentGenerationSaga;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */

@Slf4j
@Service
@Validated
@AllArgsConstructor
public class GenerationResponseMessageListenerImpl implements GenerationResponseMessageListener {
    private final DocumentGenerationSaga documentGenerationSaga;

    @Override
    public void generationCompleted(GenerationResponse generationResponse) {
        documentGenerationSaga.execute(generationResponse);
        log.info("Document Generation Saga process operation is completed for document id: {}", generationResponse.getDocumentId());

    }

    @Override
    public void generationCancelled(GenerationResponse generationResponse) {
        documentGenerationSaga.compensate(generationResponse);
        log.info("Document Generation Saga compensate operation is completed for document id: {}", generationResponse.getDocumentId());
    }
}
