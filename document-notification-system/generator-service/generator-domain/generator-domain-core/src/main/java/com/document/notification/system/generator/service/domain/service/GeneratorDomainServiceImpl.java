package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.utils.MapperUtils;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.DocumentGenerationFailedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service that orchestrates document generation business logic.
 * Acts as an orchestrator between domain logic and secondary ports (adapters).
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
@Slf4j
@AllArgsConstructor
public class GeneratorDomainServiceImpl implements IGeneratorDomainService {

    private final IContentGenerator contentGenerator;

    @Override
    public GenerationEvent validateInitiateGenerateAndComplete(DocumentGeneration documentGeneration,
                                                               List<String> failureMessages,
                                                               Map<String, Object> additionalGenerationData) {
        String fileExtension = MapperUtils.safeOrDefault(() -> documentGeneration.getFileExtension().name(), StringUtils.EMPTY);
        documentGeneration.validateGeneration(failureMessages, fileExtension);

        if (!failureMessages.isEmpty()) {
            log.error("Document generation validation failed for generationId: {} with errors: {}",
                    documentGeneration.getGenerationId().getValue(), failureMessages);
            documentGeneration.updateStatus(GenerationStatus.GENERATION_FAILED);
            return new DocumentGenerationFailedEvent(documentGeneration, DateUtils.getZoneDateTimeByUTCZoneId(), failureMessages);
        }

        try {
            documentGeneration.initializateGeneration();
            log.info("Generating content for document generation id: {}", documentGeneration.getGenerationId().getValue());

            Map<String, Object> domainGenerationData = getGenerationData(documentGeneration);
            if (additionalGenerationData != null && !additionalGenerationData.isEmpty()) {
                domainGenerationData.putAll(additionalGenerationData);
            }

            GeneratedContent generatedContent = contentGenerator.generateContent(
                    documentGeneration.getFileExtension(),
                    domainGenerationData.get("documentId").toString(),
                    domainGenerationData.get("customerId").toString(),
                    domainGenerationData
            );

            documentGeneration.setGeneratedContent(generatedContent.getBase64Content());
            documentGeneration.updateStatus(GenerationStatus.GENERATION_COMPLETED);
            log.info("Content generated successfully for generation id: {}", documentGeneration.getGenerationId().getValue());
            return new DocumentGeneratedEvent(documentGeneration, DateUtils.getZoneDateTimeByUTCZoneId());
        } catch (Exception e) {
            log.error("Failed to generate content for generation id: {}", documentGeneration.getGenerationId().getValue(), e);
            failureMessages.add("Failed to generate document content: " + e.getMessage());
            documentGeneration.updateStatus(GenerationStatus.GENERATION_FAILED);
            return new DocumentGenerationFailedEvent(documentGeneration, DateUtils.getZoneDateTimeByUTCZoneId(), failureMessages);
        }
    }

    private Map<String, Object> getGenerationData(DocumentGeneration documentGeneration) {
        log.info("Building generation data for document id: {}", documentGeneration.getDocumentId());

        Map<String, Object> generationData = new HashMap<>();
        generationData.put("generationId", documentGeneration.getGenerationId().getValue().toString());
        generationData.put("documentId", documentGeneration.getDocumentId().toString());
        generationData.put("customerId", documentGeneration.getCustomerId());
        generationData.put("fileExtension", documentGeneration.getFileExtension().name());
        return generationData;

    }
}


