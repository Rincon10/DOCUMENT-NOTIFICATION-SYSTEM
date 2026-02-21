package com.document.notification.system;

import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import com.document.notification.system.helper.IDocumentCreateHelper;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationEventPayload;
import com.document.notification.system.outbox.scheduler.generator.GeneratorOutboxHelper;
import com.document.notification.system.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final GeneratorOutboxHelper generatorOutboxHelper;

    public DocumentCreateCommandHandler(IDocumentCreateHelper documentCreateHelper, IDocumentDataMapper documentDataMapper, GeneratorOutboxHelper generatorOutboxHelper) {
        this.documentCreateHelper = documentCreateHelper;
        this.documentDataMapper = documentDataMapper;
        this.generatorOutboxHelper = generatorOutboxHelper;
    }

    @Transactional
    public CreateDocumentResponse createDocument(CreateDocumentCommand createDocumentCommand) {
        DocumentCreatedEvent documentCreatedEvent = documentCreateHelper.persistDocument(createDocumentCommand);
        log.info("Document summary is created with id: {}", documentCreatedEvent.getDocument().getId().getValue());

        CreateDocumentResponse createDocumentResponse = documentDataMapper.documentToCreateDocumentResponse(documentCreatedEvent.getDocument(), "Document created successfully");
        SagaStatus sagaStatus = null;
        DocumentGenerationEventPayload documentGenerationEventPayload = documentDataMapper.documentCreatedEventToDocumentGenerationEventPayload(documentCreatedEvent);

        generatorOutboxHelper.saveGenerationOutboxMessage(documentGenerationEventPayload,
                documentCreatedEvent.getDocument().getDocumentStatus(),
                        sagaStatus,
                        OutboxStatus.STARTED,
                        UUID.randomUUID());


        log.info("CreateDocumentResponse: {}", createDocumentResponse);
        return createDocumentResponse;

    }

}
