package com.document.notification.system.document.adapter;

import com.document.notification.system.document.entity.DocumentEntity;
import com.document.notification.system.document.mapper.DocumentDataAccessMapperI;
import com.document.notification.system.document.repository.DocumentJpaRepository;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.ports.output.repository.DocumentRepository;
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
    private final DocumentDataAccessMapperI documentDataAccessMapper;

    public DocumentRepositoryImpl(DocumentJpaRepository documentJpaRepository, DocumentDataAccessMapperI documentDataAccessMapper) {
        this.documentJpaRepository = documentJpaRepository;
        this.documentDataAccessMapper = documentDataAccessMapper;
    }

    @Override
    public Document save(Document document) {
        DocumentEntity documentEntity = documentDataAccessMapper.documentToDocumentEntity(document);
        DocumentEntity savedEntity = documentJpaRepository.save(documentEntity);
        return documentDataAccessMapper.documentEntityToDocument(savedEntity);
    }

    @Override
    public Optional<Document> findById(DocumentId orderId) {
        Optional<Document> optionalDocument = Optional.empty();

        Optional<DocumentEntity> documentEntity = documentJpaRepository.findById(orderId.getValue());
        if (documentEntity.isPresent()) {
            Document document = documentDataAccessMapper.documentEntityToDocument(documentEntity.get());
            return Optional.of(document);
        }
        return optionalDocument;
    }
}
