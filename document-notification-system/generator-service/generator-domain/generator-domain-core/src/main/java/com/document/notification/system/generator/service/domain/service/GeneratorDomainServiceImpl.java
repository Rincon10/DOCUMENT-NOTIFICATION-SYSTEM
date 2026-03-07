package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.utils.MapperUtils;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.DocumentGenerationFailedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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
                                                               GenerationContentData additionalGenerationData) {
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

            GenerationContentData generationContentData = mergeGenerationData(
                    getGenerationData(documentGeneration),
                    additionalGenerationData
            );

            GeneratedContent generatedContent = contentGenerator.generateContent(
                    documentGeneration.getFileExtension(),
                    generationContentData.getDocumentId(),
                    generationContentData.getCustomerId(),
                    generationContentData
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

    private GenerationContentData getGenerationData(DocumentGeneration documentGeneration) {
        log.info("Building generation data for document id: {}", documentGeneration.getDocumentId());

        return GenerationContentData.builder()
                .generationId(documentGeneration.getGenerationId().getValue().toString())
                .documentId(documentGeneration.getDocumentId().toString())
                .customerId(documentGeneration.getCustomerId())
                .fileExtension(documentGeneration.getFileExtension().name())
                .build();
    }

    private GenerationContentData mergeGenerationData(GenerationContentData domainData,
                                                      GenerationContentData additionalData) {
        if (additionalData == null) {
            return domainData;
        }

        return domainData.toBuilder()
                .requestId(additionalData.getRequestId())
                .sagaId(additionalData.getSagaId())
                .build();

    }
}


