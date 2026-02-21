package com.document.notification.system.saga;

import com.document.notification.system.domain.valueobject.DocumentStatus;

public interface IDocumentSagaHelper {

    SagaStatus documentStatusToSagaStatus(DocumentStatus documentStatus);
}
