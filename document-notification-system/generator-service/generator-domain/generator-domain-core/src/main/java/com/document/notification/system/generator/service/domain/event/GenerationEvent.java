package com.document.notification.system.generator.service.domain.event;

import com.document.notification.system.domain.events.DomainEvent;
import com.document.notification.system.generator.service.domain.valueobject.DocumentGeneration;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
@AllArgsConstructor
public abstract class GenerationEvent implements DomainEvent<DocumentGeneration> {
    private final DocumentGeneration documentGeneration;
    private final ZonedDateTime createdAt;
    private final List<String> failureMessages;

}
