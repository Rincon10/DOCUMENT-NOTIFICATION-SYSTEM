package com.document.notification.system.document.mapper;

import com.document.notification.system.document.entity.DocumentEntity;
import com.document.notification.system.document.service.domain.entity.Document;

public interface DocumentDataAccessMapperI {

    DocumentEntity documentToDocumentEntity(Document document);

    Document documentEntityToDocument(DocumentEntity documentEntity);
}
