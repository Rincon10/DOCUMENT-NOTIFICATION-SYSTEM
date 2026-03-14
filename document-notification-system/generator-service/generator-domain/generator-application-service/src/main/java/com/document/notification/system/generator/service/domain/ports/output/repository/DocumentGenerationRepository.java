package com.document.notification.system.generator.service.domain.ports.output.repository;

import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;

import java.util.Optional;
import java.util.UUID;

public interface DocumentGenerationRepository {
    DocumentGeneration save(DocumentGeneration documentGeneration);
}
