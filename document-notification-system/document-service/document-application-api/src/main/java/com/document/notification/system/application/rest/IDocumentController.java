package com.document.notification.system.application.rest;

import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import org.springframework.http.ResponseEntity;

public interface IDocumentController {
    ResponseEntity<CreateDocumentResponse> createDocument(CreateDocumentCommand createDocumentCommand);
}
