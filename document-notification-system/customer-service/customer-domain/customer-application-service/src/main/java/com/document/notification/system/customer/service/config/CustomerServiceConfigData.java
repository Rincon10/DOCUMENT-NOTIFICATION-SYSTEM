package com.document.notification.system.customer.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */

@Configuration
@ConfigurationProperties(prefix = "customer-service")
public class CustomerServiceConfigData {
    @Getter
    @Setter
    private String customerTopicName;
}
