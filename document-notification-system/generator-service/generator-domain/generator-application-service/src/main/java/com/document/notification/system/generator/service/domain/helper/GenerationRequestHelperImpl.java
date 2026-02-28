package com.document.notification.system.generator.service.domain.helper;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.mapper.GenerationDataMapper;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.generator.service.domain.service.IGeneratorDomainService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GenerationRequestHelperImpl implements GenerationRequestHelper {
    private final IGeneratorDomainService iGeneratorDomainService;
    private final GenerationDataMapper generationDataMapper;
    private final DocumentGenerationRepository documentGenerationRepository;
    private final DocumentOutboxRepository documentOutboxRepository;
    private final GenerationResponseMessagePublisher generationResponseMessagePublisher;

    @Transactional
    @Override
    public void persistGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    @Override
    public void persistCancelledGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
