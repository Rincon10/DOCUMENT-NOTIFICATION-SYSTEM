package com.document.notification.system.generator.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@EnableJpaRepositories(basePackages = {
        "com.document.notification.system.generator.service.dataaccess"})
@EntityScan(basePackages = {
        "com.document.notification.system.generator.service.dataaccess"})
@SpringBootApplication(scanBasePackages = "com.document.notification.system")
public class GeneratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeneratorServiceApplication.class, args);
    }
}
