package com.document.notification.system.notification.service.config;


import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.document.notification.system.notification.service.adapter.AzureEmailNotificationSender;
import com.document.notification.system.notification.service.adapter.EmailNotificationSender;
import com.document.notification.system.notification.service.adapter.EmailRateLimiter;
import com.document.notification.system.notification.service.adapter.SmtpTransportManager;
import com.document.notification.system.notification.service.domain.service.INotificationDomainService;
import com.document.notification.system.notification.service.domain.service.INotificationSender;
import com.document.notification.system.notification.service.domain.service.NotificationDomainServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;

/**
 * Wires the {@link INotificationSender} port to one of its adapters based on
 * {@code notification-service.mail.provider}:
 * <ul>
 *   <li>{@code azure} (default) — {@link AzureEmailNotificationSender}: Azure Communication
 *       Services Email, intended for high-volume delivery</li>
 *   <li>{@code smtp} — {@link EmailNotificationSender}: Gmail or any SMTP server</li>
 * </ul>
 * New providers only need a new adapter plus a conditional bean here — the
 * domain depends exclusively on the port.
 */
@Configuration
public class BeanConfiguration {

    private static final String MAIL_PROVIDER_PROPERTY = "notification-service.mail.provider";

    @Bean
    public EmailRateLimiter emailRateLimiter(
            @Value("${notification-service.mail.rate-limit.tokens-per-interval:5}") int tokensPerInterval,
            @Value("${notification-service.mail.rate-limit.refill-interval-ms:20000}") long refillIntervalMs) {
        return new EmailRateLimiter(tokensPerInterval, refillIntervalMs);
    }

    @Bean(initMethod = "validateConnection")
    @ConditionalOnProperty(name = MAIL_PROVIDER_PROPERTY, havingValue = "smtp")
    public SmtpTransportManager smtpTransportManager(JavaMailSender javaMailSender) {
        return new SmtpTransportManager((JavaMailSenderImpl) javaMailSender);
    }

    @Bean
    @ConditionalOnProperty(name = MAIL_PROVIDER_PROPERTY, havingValue = "smtp")
    public INotificationSender iNotificationSender(JavaMailSender javaMailSender,
                                                    @Value("${notification-service.mail.from}") String fromAddress,
                                                    EmailRateLimiter emailRateLimiter,
                                                    SmtpTransportManager smtpTransportManager) {
        return new EmailNotificationSender(javaMailSender, fromAddress, emailRateLimiter, smtpTransportManager);
    }

    @Bean
    @ConditionalOnProperty(name = MAIL_PROVIDER_PROPERTY, havingValue = "azure", matchIfMissing = true)
    public EmailClient azureEmailClient(
            @Value("${notification-service.mail.azure.connection-string}") String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException(
                    "ACS_CONNECTION_STRING is required when MAIL_PROVIDER=azure (the default). "
                            + "Set it to your Azure Communication Services connection string, "
                            + "or set MAIL_PROVIDER=smtp to use Gmail/SMTP instead.");
        }
        return new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Bean
    @ConditionalOnProperty(name = MAIL_PROVIDER_PROPERTY, havingValue = "azure", matchIfMissing = true)
    public INotificationSender azureNotificationSender(EmailClient azureEmailClient,
                                                       @Value("${notification-service.mail.from}") String fromAddress,
                                                       @Value("${notification-service.mail.azure.operation-timeout-seconds:60}") long operationTimeoutSeconds,
                                                       EmailRateLimiter emailRateLimiter) {
        return new AzureEmailNotificationSender(azureEmailClient, fromAddress, emailRateLimiter,
                Duration.ofSeconds(operationTimeoutSeconds));
    }

    @Bean
    public INotificationDomainService iNotificationDomainService(INotificationSender notificationSender) {
        return new NotificationDomainServiceImpl(notificationSender);
    }
}
