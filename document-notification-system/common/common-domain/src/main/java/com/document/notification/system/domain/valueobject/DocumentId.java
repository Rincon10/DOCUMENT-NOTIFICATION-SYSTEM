package com.document.notification.system.domain.valueobject;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
public class DocumentId extends BaseId<UUID> {
    public DocumentId(UUID value) {
        super(value);
    }
}
