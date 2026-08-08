package com.document.notification.system.notification.service.adapter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Manages SMTP email sending with connection reuse.
 * <p>
 * Uses {@link JavaMailSenderImpl#send(MimeMessage...)} which internally opens a
 * single SMTP connection, authenticates once, sends the message, and closes.
 * <p>
 * Additionally validates SMTP credentials at startup via {@link #validateConnection()}
 * to fail fast instead of discovering auth errors at runtime.
 * <p>
 * Implements {@link DisposableBean} for Spring lifecycle integration.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
public class SmtpTransportManager implements DisposableBean {

    private final JavaMailSenderImpl mailSender;

    public SmtpTransportManager(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Validates SMTP credentials at startup.
     * Throws if the connection or authentication fails.
     */
    public void validateConnection() {
        try {
            mailSender.testConnection();
            log.info("SMTP connection validated successfully to {}:{}", mailSender.getHost(), mailSender.getPort());
        } catch (MessagingException e) {
            log.error("SMTP connection validation failed to {}:{} - {}", mailSender.getHost(), mailSender.getPort(), e.getMessage());
            throw new IllegalStateException("Cannot connect to SMTP server: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a single email using Spring's JavaMailSender.
     * Spring handles the full SMTP lifecycle: connect → STARTTLS → AUTH → SEND → QUIT.
     */
    public void send(MimeMessage message) throws MessagingException {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new MessagingException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        log.info("SMTP transport manager shut down");
    }
}
