package com.document.notification.system.document.mapper;

import com.document.notification.system.document.entity.DocumentEntity;
import com.document.notification.system.entity.Document;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/11/2025
 */
@Component
public class DocumentDataAccessMapperImpl implements DocumentDataAccessMapperI {
    @Override
    public DocumentEntity documentToDocumentEntity(Document document) {
        return null;
    }

    @Override
    public Document documentEntityToDocument(DocumentEntity documentEntity) {
        return null;
    }
}
