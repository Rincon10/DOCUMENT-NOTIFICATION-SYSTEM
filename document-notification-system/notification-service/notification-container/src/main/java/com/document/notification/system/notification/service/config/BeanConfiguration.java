package com.document.notification.system.notification.service.config;


import com.document.notification.system.notification.service.adapter.EmailNotificationSender;
import com.document.notification.system.notification.service.adapter.EmailRateLimiter;
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
    public EmailRateLimiter emailRateLimiter(
            @Value("${notification-service.mail.rate-limit.tokens-per-interval:2}") int tokensPerInterval,
            @Value("${notification-service.mail.rate-limit.refill-interval-ms:1000}") long refillIntervalMs) {
        return new EmailRateLimiter(tokensPerInterval, refillIntervalMs);
    }

    @Bean
    public INotificationSender iNotificationSender(JavaMailSender javaMailSender,
                                                    @Value("${notification-service.mail.from}") String fromAddress,
                                                    EmailRateLimiter emailRateLimiter) {
        return new EmailNotificationSender(javaMailSender, fromAddress, emailRateLimiter);
    }

    @Bean
    public INotificationDomainService iNotificationDomainService(INotificationSender notificationSender) {
        return new NotificationDomainServiceImpl(notificationSender);
    }
}
