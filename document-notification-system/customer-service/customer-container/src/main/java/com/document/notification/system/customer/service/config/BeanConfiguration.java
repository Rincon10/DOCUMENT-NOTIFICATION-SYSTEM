package com.document.notification.system.customer.service.config;

import com.document.notification.system.customer.service.CustomerDomainService;
import com.document.notification.system.customer.service.CustomerDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 23/11/2025
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public CustomerDomainService customerDomainService() {
        return new CustomerDomainServiceImpl();
    }
}
