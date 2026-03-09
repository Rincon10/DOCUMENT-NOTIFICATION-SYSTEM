package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;

import java.util.List;

public interface IGeneratorDomainService {
    GenerationEvent validateInitiateGenerateAndComplete(DocumentGeneration documentGeneration,
                                                        List<String> failureMessages,
                                                        GenerationContentData additionalGenerationData);
}
