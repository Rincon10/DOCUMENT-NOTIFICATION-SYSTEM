package com.document.notification.system.generator.service.domain.event;

import com.document.notification.system.generator.service.domain.valueobject.DocumentGeneration;

import java.time.ZonedDateTime;
import java.util.Collections;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
public class DocumentGeneratedEvent extends GenerationEvent {

    public DocumentGeneratedEvent(DocumentGeneration documentGeneration, ZonedDateTime createdAt) {
        super(documentGeneration, createdAt, Collections.emptyList());
    }
}
