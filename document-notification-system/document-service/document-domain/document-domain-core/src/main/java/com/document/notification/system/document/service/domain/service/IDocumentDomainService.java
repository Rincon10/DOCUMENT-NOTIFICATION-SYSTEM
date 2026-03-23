package com.document.notification.system.document.service.domain.service;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCancelledEvent;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.event.DocumentGeneratedEvent;

import java.util.List;

public interface IDocumentDomainService {
    DocumentCreatedEvent validateAndInitiateDocument(Document document);

    DocumentGeneratedEvent generateDocument(Document document);

    void notificateDocument(Document document);

    DocumentCancelledEvent cancelDocumentGeneration(Document document, List<String> errors);

    void cancelDocument(Document document, List<String> errors);



}
