package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;

import java.util.List;

public interface IGeneratorDomainService {
    GenerationEvent validateAndInitiateDocumentGeneration(DocumentGeneration documentGeneration, List<String> failureMessages);
}
