package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;

import java.util.List;
import java.util.Map;

public interface IGeneratorDomainService {
    GenerationEvent validateInitiateGenerateAndComplete(DocumentGeneration documentGeneration,
                                                        List<String> failureMessages,
                                                        Map<String, Object> additionalGenerationData);
}
