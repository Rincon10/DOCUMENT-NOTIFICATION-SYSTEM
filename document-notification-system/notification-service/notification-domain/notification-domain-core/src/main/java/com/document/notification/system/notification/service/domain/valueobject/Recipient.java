package com.document.notification.system.notification.service.domain.valueobject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Recipient {
    private final String target;
    private final NotificationChannel channel;
}

