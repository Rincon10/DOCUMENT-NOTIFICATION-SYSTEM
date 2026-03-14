package com.document.notification.system.saga;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public interface IDocumentSagaHelper {

    SagaStatus documentStatusToSagaStatus(DocumentStatus documentStatus);

    Document findDocument(@NotNull String documentId);
}
