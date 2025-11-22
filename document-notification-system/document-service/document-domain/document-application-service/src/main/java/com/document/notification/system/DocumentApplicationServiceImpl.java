package com.document.notification.system;

import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import com.document.notification.system.ports.input.service.DocumentApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Slf4j
@Validated
@Service
public class DocumentApplicationServiceImpl implements DocumentApplicationService {
    private final DocumentCreateCommandHandler documentCreateCommandHandler;

    public DocumentApplicationServiceImpl(DocumentCreateCommandHandler documentCreateCommandHandler) {
        this.documentCreateCommandHandler = documentCreateCommandHandler;
    }

    @Override
    public CreateDocumentResponse createDocument(CreateDocumentCommand createDocumentCommand) {
        return documentCreateCommandHandler.createDocument(createDocumentCommand);
    }
}
