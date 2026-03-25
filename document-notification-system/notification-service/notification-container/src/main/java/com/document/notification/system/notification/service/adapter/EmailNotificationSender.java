package com.document.notification.system.notification.service.adapter;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import com.document.notification.system.notification.service.domain.service.INotificationSender;
import com.document.notification.system.notification.service.domain.valueobject.NotificationChannel;
import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationResult;
import com.document.notification.system.notification.service.domain.valueobject.Recipient;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Infrastructure adapter that implements the domain port INotificationSender
 * using Spring's JavaMailSender for real SMTP email delivery.
 * <p>
 * Applies three resilience patterns:
 * <ul>
 *   <li><b>Token Bucket Rate Limiting</b> — controls throughput via {@link EmailRateLimiter}
 *       so the SMTP server is never saturated.</li>
 *   <li><b>Exponential Backoff with Jitter</b> — on transient SMTP failures, retries with
 *       increasing delays plus random jitter to avoid thundering-herd effects.</li>
 *   <li><b>SMTP Connection Reuse</b> — keeps a persistent Transport connection to avoid
 *       repeated login attempts that trigger Gmail's "Too many login attempts" error.</li>
 * </ul>
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
public class EmailNotificationSender implements INotificationSender {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    private final JavaMailSender javaMailSender;
    private final String fromAddress;
    private final EmailRateLimiter rateLimiter;

    private final Object transportLock = new Object();
    private volatile Transport sharedTransport;

    public EmailNotificationSender(JavaMailSender javaMailSender,
                                    String fromAddress,
                                    EmailRateLimiter rateLimiter) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public NotificationResult sendNotification(Recipient recipient,
                                                NotificationContent notificationContent,
                                                NotificationData data) {
        log.info("Sending {} notification to recipient: {} for document: {}",
                recipient.getChannel(), recipient.getTarget(), data.getDocumentId());

        if (recipient.getChannel() != NotificationChannel.EMAIL) {
            throw new NotificationDomainException(
                    "Unsupported notification channel: " + recipient.getChannel());
        }

        acquireRateLimitToken(recipient, data);
        return sendEmailWithRetry(recipient, notificationContent, data);
    }

    private void acquireRateLimitToken(Recipient recipient, NotificationData data) {
        try {
            log.debug("Acquiring rate limit token for document: {}", data.getDocumentId());
            rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationDomainException(
                    "Interrupted while waiting for email rate limit token for " + recipient.getTarget());
        }
    }

    /**
     * Sends email with Exponential Backoff + Jitter retry strategy.
     * Delay formula: baseDelay * 2^(attempt-1) + random jitter
     * Example: attempt 1 = ~1s, attempt 2 = ~2s, attempt 3 = ~4s
     */
    private NotificationResult sendEmailWithRetry(Recipient recipient,
                                                   NotificationContent notificationContent,
                                                   NotificationData data) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Attempt {}/{} - Sending email to: {} for document: {}",
                        attempt, MAX_RETRIES, recipient.getTarget(), data.getDocumentId());

                NotificationResult result = doSendEmail(recipient, notificationContent, data);

                log.info("Email sent successfully to: {} | MessageId: {} | Attempt: {}",
                        recipient.getTarget(), result.getMessageId(), attempt);
                return result;

            } catch (MessagingException | MailException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for document: {} - Error: {}",
                        attempt, MAX_RETRIES, data.getDocumentId(), e.getMessage());

                resetTransport();

                if (attempt < MAX_RETRIES) {
                    sleepWithBackoff(attempt, recipient.getTarget());
                }
            }
        }

        log.error("All {} attempts failed to send email to: {} for document: {}",
                MAX_RETRIES, recipient.getTarget(), data.getDocumentId(), lastException);
        throw new NotificationDomainException(
                "Failed to send email to " + recipient.getTarget() + " after " + MAX_RETRIES
                        + " attempts: " + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    private NotificationResult doSendEmail(Recipient recipient,
                                            NotificationContent notificationContent,
                                            NotificationData data) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        boolean hasAttachment = notificationContent.getContentBase64() != null
                && notificationContent.getFileName() != null;

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachment, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(recipient.getTarget());
        helper.setSubject(notificationContent.getSubject());

        String htmlBody = buildHtmlBody(notificationContent, data);
        helper.setText(htmlBody, true);

        if (hasAttachment) {
            byte[] decodedContent = Base64.getDecoder().decode(notificationContent.getContentBase64());
            String mimeType = resolveAttachmentMimeType(notificationContent.getContentType());
            helper.addAttachment(
                    notificationContent.getFileName(),
                    new ByteArrayDataSource(decodedContent, mimeType)
            );
        }

        sendWithReusableTransport(mimeMessage);

        String messageId = mimeMessage.getMessageID();
        if (messageId == null || messageId.isBlank()) {
            messageId = UUID.randomUUID().toString();
            log.warn("SMTP server did not return a messageId, generated fallback: {}", messageId);
        }

        return new NotificationResult(
                true,
                messageId,
                NotificationChannel.EMAIL,
                recipient.getTarget(),
                "Email delivered successfully to " + recipient.getTarget()
        );
    }

    /**
     * Sends email reusing a persistent SMTP Transport connection.
     * Only creates a new connection (login) when the existing one is closed or missing.
     * This avoids Gmail's "Too many login attempts" error on bulk sends.
     */
    private void sendWithReusableTransport(MimeMessage mimeMessage) throws MessagingException {
        synchronized (transportLock) {
            Transport transport = getOrCreateTransport();
            try {
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
            } catch (MessagingException e) {
                log.warn("Failed to send with reused transport, resetting connection: {}", e.getMessage());
                closeTransportQuietly();
                throw e;
            }
        }
    }

    private Transport getOrCreateTransport() throws MessagingException {
        if (sharedTransport != null && sharedTransport.isConnected()) {
            return sharedTransport;
        }

        if (!(javaMailSender instanceof JavaMailSenderImpl mailSenderImpl)) {
            throw new NotificationDomainException(
                    "JavaMailSender must be an instance of JavaMailSenderImpl for connection reuse");
        }

        Session session = mailSenderImpl.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect(
                mailSenderImpl.getHost(),
                mailSenderImpl.getPort(),
                mailSenderImpl.getUsername(),
                mailSenderImpl.getPassword()
        );

        sharedTransport = transport;
        log.info("New SMTP transport connection established to {}:{}", mailSenderImpl.getHost(), mailSenderImpl.getPort());
        return transport;
    }

    private void resetTransport() {
        synchronized (transportLock) {
            closeTransportQuietly();
        }
    }

    private void closeTransportQuietly() {
        if (sharedTransport != null) {
            try {
                sharedTransport.close();
            } catch (MessagingException e) {
                log.debug("Error closing transport: {}", e.getMessage());
            }
            sharedTransport = null;
        }
    }

    /**
     * Exponential Backoff with Jitter: delay = base * 2^(attempt-1) + random(0, base)
     */
    private void sleepWithBackoff(int attempt, String target) {
        long exponentialDelay = BASE_BACKOFF_MS * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, BASE_BACKOFF_MS);
        long totalDelay = exponentialDelay + jitter;

        log.info("Waiting {}ms before retry (backoff + jitter) for {}", totalDelay, target);
        try {
            Thread.sleep(totalDelay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new NotificationDomainException(
                    "Email sending interrupted during backoff for " + target);
        }
    }

    private String buildHtmlBody(NotificationContent notificationContent, NotificationData data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family: Arial, sans-serif; margin: 20px;\">");

        html.append("<div style=\"background-color: #f0f0f0; padding: 15px; border-radius: 5px;\">");
        html.append("<h2 style=\"margin: 0;\">").append(notificationContent.getSubject()).append("</h2>");
        html.append("</div>");

        html.append("<div style=\"margin-top: 15px;\">");
        html.append(notificationContent.getMessage());
        html.append("</div>");

        html.append("<hr style=\"margin-top: 20px;\">");
        html.append("<table style=\"font-size: 12px; color: #666;\">");
        appendRow(html, "Document ID", data.getDocumentId());
        appendRow(html, "Customer ID", data.getCustomerId());
        appendRow(html, "Notification ID", data.getNotificationId());
        appendRow(html, "Saga ID", data.getSagaId());
        html.append("</table>");

        if (notificationContent.getFileName() != null) {
            html.append("<p style=\"font-size: 12px; color: #666;\">")
                    .append("Attached: ").append(notificationContent.getFileName())
                    .append("</p>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private void appendRow(StringBuilder html, String label, String value) {
        if (value != null) {
            html.append("<tr><td style=\"padding: 2px 8px;\"><strong>")
                    .append(label).append(":</strong></td><td>")
                    .append(value).append("</td></tr>");
        }
    }

    private String resolveAttachmentMimeType(String contentType) {
        String mimeType = DocumentType.resolveMimeType(contentType);
        return mimeType != null ? mimeType : "application/octet-stream";
    }
}
