package com.document.notification.system.adapter;

import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.entity.Document;
import com.document.notification.system.ports.output.repository.DocumentRepository;
import com.document.notification.system.repository.DocumentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Component
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository documentJpaRepository;

    public DocumentRepositoryImpl(DocumentJpaRepository documentJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public Document save(Document document) {
        return null;
    }

    @Override
    public Optional<Document> findById(DocumentId orderId) {
        return Optional.empty();
    }
}
