package com.document.notification.system.ports.output.repository;

import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;

public interface GeneratorOutboxRepository {


    DocumentGenerationOutboxMessage save(DocumentGenerationOutboxMessage documentGenerationOutboxMessage);
}
