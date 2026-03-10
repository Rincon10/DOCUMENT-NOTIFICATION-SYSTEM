package com.document.notification.system.notification.service.domain.valueobject;

import java.util.UUID;

public class NotificationId {
    private final UUID value;

    public NotificationId(UUID value) {
        this.value = value;
    }

    public UUID getValue() {
        return value;
    }
}

