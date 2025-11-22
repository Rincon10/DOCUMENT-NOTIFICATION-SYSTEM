package com.document.notification.system.document.service.config;

import com.document.notification.system.document.service.domain.service.DocumentDomainServiceI;
import com.document.notification.system.document.service.domain.service.DocumentDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public DocumentDomainServiceI documentDomainServiceI() {
        return new DocumentDomainServiceImpl();
    }
}
