package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;

/**
 * Service interface for generating document content in different formats
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
public interface IContentGenerator {

    /**
     * Generates document content in Base64 format
     *
     * @param documentType The type of document to generate (PDF, HTML, etc.)
     * @param documentId   The unique identifier of the document
     * @param customerId   The customer identifier
     * @param data         Additional data needed for generation
     * @return GeneratedContent object containing Base64 content
     */
    GeneratedContent generateContent(DocumentType documentType,
                                    String documentId,
                                    String customerId,
                                    GenerationContentData data);
}

