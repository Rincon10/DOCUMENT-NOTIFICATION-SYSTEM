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
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Infrastructure adapter that sends email notifications via SMTP.
 * <p>
 * Composes three resilience mechanisms:
 * <ul>
 *   <li>{@link EmailRateLimiter} — Token Bucket that throttles throughput</li>
 *   <li>{@link SmtpTransportManager} — Reuses a single SMTP connection to avoid
 *       repeated logins (prevents "Too many login attempts")</li>
 *   <li>Exponential Backoff with Jitter — retries transient SMTP failures</li>
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
    private final SmtpTransportManager transportManager;

    public EmailNotificationSender(JavaMailSender javaMailSender,
                                    String fromAddress,
                                    EmailRateLimiter rateLimiter,
                                    SmtpTransportManager transportManager) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
        this.rateLimiter = rateLimiter;
        this.transportManager = transportManager;
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

        acquireRateLimitToken(data);
        return sendEmailWithRetry(recipient, notificationContent, data);
    }

    private void acquireRateLimitToken(NotificationData data) {
        try {
            log.debug("Acquiring rate limit token for document: {}", data.getDocumentId());
            rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationDomainException("Interrupted while waiting for email rate limit token");
        }
    }

    private NotificationResult sendEmailWithRetry(Recipient recipient,
                                                   NotificationContent notificationContent,
                                                   NotificationData data) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Attempt {}/{} - Sending email to: {} for document: {}",
                        attempt, MAX_RETRIES, recipient.getTarget(), data.getDocumentId());

                MimeMessage message = buildMimeMessage(recipient, notificationContent, data);
                transportManager.send(message);

                String messageId = resolveMessageId(message);
                log.info("Email sent successfully to: {} | MessageId: {} | Attempt: {}",
                        recipient.getTarget(), messageId, attempt);

                return new NotificationResult(
                        true, messageId, NotificationChannel.EMAIL,
                        recipient.getTarget(),
                        "Email delivered successfully to " + recipient.getTarget()
                );

            } catch (MessagingException | MailException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for document: {} - Error: {}",
                        attempt, MAX_RETRIES, data.getDocumentId(), e.getMessage());

                if (attempt < MAX_RETRIES) {
                    sleepWithBackoff(attempt);
                }
            }
        }

        log.error("All {} attempts failed to send email to: {} for document: {}",
                MAX_RETRIES, recipient.getTarget(), data.getDocumentId(), lastException);
        throw new NotificationDomainException(
                "Failed to send email to " + recipient.getTarget() + " after " + MAX_RETRIES
                        + " attempts: " + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    private MimeMessage buildMimeMessage(Recipient recipient,
                                          NotificationContent notificationContent,
                                          NotificationData data) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        boolean hasAttachment = notificationContent.getContentBase64() != null
                && notificationContent.getFileName() != null;

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachment, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(recipient.getTarget());
        helper.setSubject(notificationContent.getSubject());
        helper.setText(buildHtmlBody(notificationContent, data), true);

        if (hasAttachment) {
            byte[] decodedContent = Base64.getDecoder().decode(notificationContent.getContentBase64());
            String mimeType = resolveAttachmentMimeType(notificationContent.getContentType());
            helper.addAttachment(notificationContent.getFileName(),
                    new ByteArrayDataSource(decodedContent, mimeType));
        }

        return mimeMessage;
    }

    private String resolveMessageId(MimeMessage message) throws MessagingException {
        String messageId = message.getMessageID();
        if (messageId == null || messageId.isBlank()) {
            messageId = UUID.randomUUID().toString();
            log.warn("SMTP server did not return a messageId, generated fallback: {}", messageId);
        }
        return messageId;
    }

    /**
     * Exponential Backoff with Jitter: delay = base * 2^(attempt-1) + random(0, base)
     */
    private void sleepWithBackoff(int attempt) {
        long exponentialDelay = BASE_BACKOFF_MS * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, BASE_BACKOFF_MS);
        long totalDelay = exponentialDelay + jitter;

        log.info("Waiting {}ms before retry (backoff + jitter)", totalDelay);
        try {
            Thread.sleep(totalDelay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new NotificationDomainException("Email sending interrupted during backoff");
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
