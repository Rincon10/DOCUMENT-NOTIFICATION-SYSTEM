package com.document.notification.system.domain.valueobject;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
public class ItemId  extends BaseId<UUID> {
    public ItemId(UUID value) {
        super(value);
    }
}
