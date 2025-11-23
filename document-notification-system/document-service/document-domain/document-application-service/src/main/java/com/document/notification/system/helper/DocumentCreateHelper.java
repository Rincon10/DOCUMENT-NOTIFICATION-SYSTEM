package com.document.notification.system.helper;

import com.document.notification.system.document.service.domain.entity.Customer;
import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.document.service.domain.service.IDocumentDomainService;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.ports.output.repository.CustomerRepository;
import com.document.notification.system.ports.output.repository.IDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    private final CustomerRepository customerRepository;

    public DocumentCreateHelper(IDocumentDomainService documentDomainService, IDocumentDataMapper documentDataMapper, IDocumentRepository documentRepository, CustomerRepository customerRepository) {
        this.documentDomainService = documentDomainService;
        this.documentDataMapper = documentDataMapper;
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    @Override
    public DocumentCreatedEvent persistDocument(CreateDocumentCommand createDocumentCommand) {
        checkCustomer(createDocumentCommand.getCustomerId());

        Document document = documentDataMapper.createDocumentCommandToDocument(createDocumentCommand);
        DocumentCreatedEvent documentCreatedEvent = documentDomainService.validateAndInitiateDocument(document);

        saveDocument(document);
        return documentCreatedEvent;
    }

    private void checkCustomer(UUID customerId) {
        Optional<Customer> customer = customerRepository.findCustomer(customerId);
        if (customer.isEmpty()) {
            log.warn("Could not find customer with customer id: {}", customerId);
            throw new DocumentDomainException("Could not find customer with customer id: " + customer);
        }
    }

    private Document saveDocument(Document document) {
        Document savedDocument = documentRepository.save(document);
        if (Objects.isNull(savedDocument) ) {
            log.error("Could not save document with id {}", document.getId().getValue());
            throw new DocumentDomainException("Could not save  the document.");
        }

        log.info("Document with id {} is saved successfully", savedDocument.getId().getValue());
        return savedDocument;
    }
}
