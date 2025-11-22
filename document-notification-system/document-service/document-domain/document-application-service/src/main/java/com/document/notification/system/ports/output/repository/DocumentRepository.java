package com.document.notification.system.ports.output.repository;

import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.document.service.domain.entity.Document;

import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);

    Optional<Document> findById(DocumentId orderId);

}
