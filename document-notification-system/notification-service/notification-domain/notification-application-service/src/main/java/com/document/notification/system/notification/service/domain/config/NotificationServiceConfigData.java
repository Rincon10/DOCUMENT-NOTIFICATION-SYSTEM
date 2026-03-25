package com.document.notification.system.notification.service.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification-service")
public class NotificationServiceConfigData {
    private String instanceId;
    private String notificationRequestTopicName;
    private String notificationResponseTopicName;
}
