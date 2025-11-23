package com.document.notification.system.helper;

import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.dto.create.CreateDocumentCommand;

public interface IDocumentCreateHelper {

    DocumentCreatedEvent persistDocument(CreateDocumentCommand createDocumentCommand);
}
