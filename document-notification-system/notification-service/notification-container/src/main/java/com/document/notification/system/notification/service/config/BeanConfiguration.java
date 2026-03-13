package com.document.notification.system.notification.service.config;

import com.document.notification.system.notification.service.domain.service.INotificationDomainService;
import com.document.notification.system.notification.service.domain.service.NotificationDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public INotificationDomainService iNotificationDomainService() {
        return new NotificationDomainServiceImpl();
    }
}
