package com.document.notification.system.notification.service.config;

import com.document.notification.system.notification.service.adapter.EmailNotificationSender;
import com.document.notification.system.notification.service.domain.service.INotificationDomainService;
import com.document.notification.system.notification.service.domain.service.INotificationSender;
import com.document.notification.system.notification.service.domain.service.NotificationDomainServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class BeanConfiguration {

    @Bean
    public INotificationSender iNotificationSender(JavaMailSender javaMailSender,
                                                    @Value("${notification-service.mail.from}") String fromAddress) {
        return new EmailNotificationSender(javaMailSender, fromAddress);
    }

    @Bean
    public INotificationDomainService iNotificationDomainService(INotificationSender notificationSender) {
        return new NotificationDomainServiceImpl(notificationSender);
    }
}
