package com.document.notification.system.generator.service.dataaccess.mapper;

import com.document.notification.system.generator.service.dataaccess.entity.DocumentGenerationEntity;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
public interface IDocumentGenerationDataMapper {
    DocumentGenerationEntity documentGenerationToDocumentGenerationEntity(DocumentGeneration documentGeneration);
}
