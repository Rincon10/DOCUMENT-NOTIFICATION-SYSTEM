package com.document.notification.system.generator.service.dataaccess.adapter;

import com.document.notification.system.generator.service.dataaccess.mapper.IDocumentGenerationDataMapper;
import com.document.notification.system.generator.service.dataaccess.repository.DocumentGenerationJpaRepository;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
@Component
@AllArgsConstructor
public class DocumentGenerationRepositoryImpl implements DocumentGenerationRepository {

    private final DocumentGenerationJpaRepository documentGenerationJpaRepository;
    private final IDocumentGenerationDataMapper documentGenerationDataMapper;

    @Override
    public DocumentGeneration save(DocumentGeneration documentGeneration) {
        documentGenerationDataMapper.documentGenerationToDocumentGenerationEntity(documentGeneration);
        return documentGeneration;
    }

    @Override
    public Optional<DocumentGeneration> findByDocumentId(UUID documentId) {
        return Optional.empty();
    }
}
