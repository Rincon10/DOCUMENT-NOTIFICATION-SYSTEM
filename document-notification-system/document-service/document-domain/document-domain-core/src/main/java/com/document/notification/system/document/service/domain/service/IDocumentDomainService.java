package com.document.notification.system.document.service.domain.service;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;

public interface IDocumentDomainService {
    DocumentCreatedEvent validateAndInitiateDocument(Document document);

}
