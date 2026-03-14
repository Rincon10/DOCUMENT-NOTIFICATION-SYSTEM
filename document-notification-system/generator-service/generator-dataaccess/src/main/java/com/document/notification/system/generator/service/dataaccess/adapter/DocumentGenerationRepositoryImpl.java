package com.document.notification.system.generator.service.dataaccess.adapter;

import com.document.notification.system.generator.service.dataaccess.entity.DocumentGenerationEntity;
import com.document.notification.system.generator.service.dataaccess.mapper.IDocumentGenerationDataMapper;
import com.document.notification.system.generator.service.dataaccess.repository.DocumentGenerationJpaRepository;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

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
        DocumentGenerationEntity entity = documentGenerationDataMapper.documentGenerationToDocumentGenerationEntity(documentGeneration);
        documentGenerationJpaRepository.save(entity);
        return documentGeneration;
    }
}
