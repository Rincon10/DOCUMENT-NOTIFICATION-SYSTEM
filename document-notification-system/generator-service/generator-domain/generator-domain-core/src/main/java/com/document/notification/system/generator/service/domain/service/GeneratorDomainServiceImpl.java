package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.utils.MapperUtils;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.DocumentGenerationFailedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
@Slf4j
public class GeneratorDomainServiceImpl implements IGeneratorDomainService {

    @Override
    public GenerationEvent validateAndInitiateDocumentGeneration(DocumentGeneration documentGeneration, List<String> failureMessages) {
        String fileExtension = MapperUtils.safeOrDefault(() -> documentGeneration.getFileExtension().name(), StringUtils.EMPTY);
        documentGeneration.validateGeneration(failureMessages, fileExtension);
        documentGeneration.initializateGeneration();

        if (failureMessages.isEmpty()) {
            log.info("Document generation validated and initiated successfully for generationId: {}", documentGeneration.getGenerationId().getValue());
            documentGeneration.updateStatus(GenerationStatus.GENERATION_COMPLETED);
            return new DocumentGeneratedEvent(documentGeneration, DateUtils.getZoneDateTimeByUTCZoneId());
        }
        log.error("Document generation validation failed for generationId: {} with errors: {}", documentGeneration.getGenerationId().getValue(), failureMessages);
        documentGeneration.updateStatus(GenerationStatus.GENERATION_FAILED);
        return new DocumentGenerationFailedEvent(documentGeneration, DateUtils.getZoneDateTimeByUTCZoneId(), failureMessages);
    }
}


