package com.document.notification.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "document-service")
public class DocumentServiceConfigData {
    // topics a los cual deberia publicar
}
