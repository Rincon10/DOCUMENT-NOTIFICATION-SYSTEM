package com.document.notification.system;

import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import com.document.notification.system.helper.IDocumentCreateHelper;
import com.document.notification.system.mapper.IDocumentDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Slf4j
@Component
public class DocumentCreateCommandHandler {
    private final IDocumentCreateHelper documentCreateHelper;
    private final IDocumentDataMapper documentDataMapper;

    public DocumentCreateCommandHandler(IDocumentCreateHelper documentCreateHelper, IDocumentDataMapper documentDataMapper) {
        this.documentCreateHelper = documentCreateHelper;
        this.documentDataMapper = documentDataMapper;
    }

    @Transactional
    public CreateDocumentResponse createDocument(CreateDocumentCommand createDocumentCommand) {
        DocumentCreatedEvent documentCreatedEvent = documentCreateHelper.persistDocument(createDocumentCommand);
        log.info("Document summary is created with id: {}", documentCreatedEvent.getDocument().getId().getValue());

        CreateDocumentResponse createDocumentResponse = documentDataMapper.documentToCreateDocumentResponse(documentCreatedEvent.getDocument(), "Document created successfully");

        log.info("CreateDocumentResponse: {}", createDocumentResponse);
        return createDocumentResponse;

    }

}
