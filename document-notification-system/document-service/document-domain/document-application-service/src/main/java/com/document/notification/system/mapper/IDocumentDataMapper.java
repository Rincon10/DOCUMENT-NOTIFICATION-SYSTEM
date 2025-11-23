package com.document.notification.system.mapper;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;

public interface IDocumentDataMapper {
    Document createDocumentCommandToDocument(CreateDocumentCommand createDocumentCommand);

    CreateDocumentResponse documentToCreateDocumentResponse(Document document, String message);
}
