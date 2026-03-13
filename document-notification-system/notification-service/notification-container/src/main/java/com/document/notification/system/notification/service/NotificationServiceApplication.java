package com.document.notification.system.notification.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = {
        "com.document.notification.system.notification.service.dataaccess"})
@EntityScan(basePackages = {
        "com.document.notification.system.notification.service.dataaccess"})
@SpringBootApplication(scanBasePackages = "com.document.notification.system")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
