package com.document.notification.system.document.service.domain.service;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.domain.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

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
        return new DocumentCreatedEvent(document, DateUtils.getZoneDateTimeByUTCZoneId());
    }
}
