package com.document.notification.system.document.service.domain.service;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCancelledEvent;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.domain.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Slf4j
public class DocumentDomainServiceImpl implements IDocumentDomainService {
    @Override
    public DocumentCreatedEvent validateAndInitiateDocument(Document document) {
        document.validateDocument();
        document.initializeDocument();

        log.info("Document with id: {} is initiated", document.getId().getValue());

        return new DocumentCreatedEvent(document, DateUtils.getZoneDateTimeByUTCZoneId());
    }

    @Override
    public DocumentGeneratedEvent generateDocument(Document document) {
        document.generate();
        log.info("Document with id: {} is generated", document.getId().getValue());
        return new DocumentGeneratedEvent(document, DateUtils.getZoneDateTimeByUTCZoneId());
    }

    @Override
    public void notificateDocument(Document document) {
        document.sent();
        log.info("Document with id: {} is sent it", document.getId().getValue());

    }

    @Override
    public DocumentCancelledEvent cancelDocumentGeneration(Document document, List<String> errors) {
        document.initCancel(errors);
        log.info("Document with id: {} started cancel for generation", document.getId().getValue());
        return new DocumentCancelledEvent(document, DateUtils.getZoneDateTimeByUTCZoneId());
    }

    @Override
    public void cancelDocument(Document document, List<String> errors) {
        document.cancel(errors);
        log.info("Document with id: {} is cancelled", document.getId().getValue());

    }
}
