package com.document.notification.system.ports.input.service;

import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;

public interface DocumentApplicationService {

    CreateDocumentResponse createDocument(CreateDocumentCommand createDocumentCommand);
}
