package com.document.notification.system.domain.events.publisher;

import com.document.notification.system.domain.events.DomainEvent;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
public interface DomainEventPublisher<T extends DomainEvent> {

    void publish(T domainEvent);
}