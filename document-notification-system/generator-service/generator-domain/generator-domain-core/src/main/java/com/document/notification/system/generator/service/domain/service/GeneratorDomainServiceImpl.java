package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.valueobject.DocumentGeneration;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
@Slf4j
public class GeneratorDomainServiceImpl implements IGeneratorDomainService {
    @Override
    public GenerationEvent validateAndInitiateDocumentGeneration(DocumentGeneration documentGeneration) {
        throw new RuntimeException("Not implemented yet");
    }
}
