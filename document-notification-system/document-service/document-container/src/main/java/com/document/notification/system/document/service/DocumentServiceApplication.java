package com.document.notification.system.document.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@EnableJpaRepositories(basePackages = {"com.document.notification.system.document.service.dataaccess"})
@EntityScan(basePackages = {"com.document.notification.system.document.service.dataaccess"})
@SpringBootApplication(scanBasePackages = "com.document.notification.system")
public class DocumentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
