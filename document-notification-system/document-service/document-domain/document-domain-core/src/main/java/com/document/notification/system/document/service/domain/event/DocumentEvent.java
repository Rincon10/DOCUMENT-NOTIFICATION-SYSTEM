package com.document.notification.system.document.service.domain.event;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.domain.events.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@AllArgsConstructor
public abstract class DocumentEvent implements DomainEvent<Document> {
    private final Document document;
    private final ZonedDateTime createdAt;

}
