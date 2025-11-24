package com.document.notification.system.customer.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@EnableJpaRepositories(basePackages = {
        "com.document.notification.system.customer.service.dataaccess"})
@EntityScan(basePackages = {
        "com.document.notification.system.customer.service.dataaccess"})
@SpringBootApplication(scanBasePackages = "com.document.notification.system")
public class CustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
