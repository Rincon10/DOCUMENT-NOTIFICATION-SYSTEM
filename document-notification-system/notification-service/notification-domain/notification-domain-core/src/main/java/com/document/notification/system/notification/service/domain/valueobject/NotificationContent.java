package com.document.notification.system.notification.service.domain.valueobject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationContent {
    private final String subject;
    private final String message;
    private final String fileName;
    private final String contentType;
    private final String contentBase64;
}

