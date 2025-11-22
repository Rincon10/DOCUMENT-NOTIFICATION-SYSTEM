package com.document.notification.system.document.service.dataaccess.mapper;

import com.document.notification.system.document.service.dataaccess.entity.DocumentEntity;
import com.document.notification.system.document.service.domain.entity.Document;

public interface DocumentDataAccessMapperI {

    DocumentEntity documentToDocumentEntity(Document document);

    Document documentEntityToDocument(DocumentEntity documentEntity);
}
