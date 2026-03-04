package com.document.notification.system.generator.service.config;

import com.document.notification.system.generator.service.domain.service.ContentGeneratorImpl;
import com.document.notification.system.generator.service.domain.service.GeneratorDomainServiceImpl;
import com.document.notification.system.generator.service.domain.service.IContentGenerator;
import com.document.notification.system.generator.service.domain.service.IGeneratorDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public IGeneratorDomainService iGeneratorDomainService() {
        return new GeneratorDomainServiceImpl();
    }

    @Bean
    public IContentGenerator iContentGenerator() {
        return new ContentGeneratorImpl();
    }

}

