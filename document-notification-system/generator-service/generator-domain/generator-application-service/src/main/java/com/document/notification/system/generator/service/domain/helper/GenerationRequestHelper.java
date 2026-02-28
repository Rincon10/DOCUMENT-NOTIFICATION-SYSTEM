package com.document.notification.system.generator.service.domain.helper;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;

public interface GenerationRequestHelper {
    void persistGenerationOnHistoryRecords(GenerationRequest generationRequest);

    void persistCancelledGenerationOnHistoryRecords(GenerationRequest generationRequest);


}
