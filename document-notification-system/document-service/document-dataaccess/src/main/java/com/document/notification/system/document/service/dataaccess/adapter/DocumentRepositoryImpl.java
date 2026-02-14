package com.document.notification.system.document.service.dataaccess.adapter;

import com.document.notification.system.document.service.dataaccess.entity.DocumentEntity;
import com.document.notification.system.document.service.dataaccess.mapper.DocumentDataAccessMapperI;
import com.document.notification.system.document.service.dataaccess.repository.DocumentJpaRepository;
import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.ports.output.repository.IDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Component
public class DocumentRepositoryImpl implements IDocumentRepository {

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
    public Optional<Document> findById(DocumentId documentId) {
        Optional<Document> optionalDocument = Optional.empty();

        Optional<DocumentEntity> documentEntity = documentJpaRepository.findById(documentId.getValue());
        if (documentEntity.isPresent()) {
            Document document = documentDataAccessMapper.documentEntityToDocument(documentEntity.get());
            return Optional.of(document);
        }
        return optionalDocument;
    }
}
