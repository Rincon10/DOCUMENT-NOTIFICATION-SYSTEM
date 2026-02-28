package com.document.notification.system.generator.service.domain.valueobject;

import com.document.notification.system.domain.valueobject.BaseId;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 27/02/2026
 */
public class GenerationId extends BaseId<UUID> {
    public GenerationId(UUID value) {
        super(value);
    }
}