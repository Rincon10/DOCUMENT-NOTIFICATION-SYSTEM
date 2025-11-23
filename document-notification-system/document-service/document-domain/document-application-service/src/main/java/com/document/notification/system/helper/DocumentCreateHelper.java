package com.document.notification.system.helper;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.service.IDocumentDomainService;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.ports.output.repository.IDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Slf4j
@Component
public class DocumentCreateHelper implements IDocumentCreateHelper {
    private final IDocumentDomainService documentDomainService;

    private final IDocumentDataMapper documentDataMapper;

    private final IDocumentRepository documentRepository;

    public DocumentCreateHelper(IDocumentDomainService documentDomainService, IDocumentDataMapper documentDataMapper, IDocumentRepository documentRepository) {
        this.documentDomainService = documentDomainService;
        this.documentDataMapper = documentDataMapper;
        this.documentRepository = documentRepository;
    }

    @Transactional
    @Override
    public DocumentCreatedEvent persistDocument(CreateDocumentCommand createDocumentCommand) {

        Document document = documentDataMapper.createDocumentCommandToDocument(createDocumentCommand);

        Document savedDocument = documentRepository.save(document);
        return null;
    }
}
