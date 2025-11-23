package com.document.notification.system.ports.output.repository;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.domain.valueobject.DocumentId;

import java.util.Optional;

public interface IDocumentRepository {
    Document save(Document document);

    Optional<Document> findById(DocumentId orderId);

}
