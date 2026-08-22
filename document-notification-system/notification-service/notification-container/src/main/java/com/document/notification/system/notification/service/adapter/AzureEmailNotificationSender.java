package com.document.notification.system.notification.service.adapter;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAttachment;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import com.document.notification.system.notification.service.domain.service.INotificationSender;
import com.document.notification.system.notification.service.domain.valueobject.NotificationChannel;
import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationResult;
import com.document.notification.system.notification.service.domain.valueobject.Recipient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Infrastructure adapter that sends email notifications through
 * Azure Communication Services Email (HTTPS API, no SMTP involved).
 * <p>
 * Alternative to {@link EmailNotificationSender} for high-volume delivery:
 * ACS is designed for bulk transactional email, so it needs no SMTP connection
 * reuse and tolerates much higher rate limits. Selected with
 * {@code notification-service.mail.provider=azure}.
 * <p>
 * Shares with the SMTP adapter:
 * <ul>
 *   <li>{@link EmailRateLimiter} — Token Bucket that throttles throughput</li>
 *   <li>{@link EmailContentComposer} — identical email body across providers</li>
 *   <li>Exponential Backoff with Jitter — retries transient send failures</li>
 * </ul>
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
public class AzureEmailNotificationSender implements INotificationSender {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    private final EmailClient emailClient;
    private final String fromAddress;
    private final EmailRateLimiter rateLimiter;
    private final Duration operationTimeout;

    public AzureEmailNotificationSender(EmailClient emailClient,
                                        String fromAddress,
                                        EmailRateLimiter rateLimiter,
                                        Duration operationTimeout) {
        this.emailClient = emailClient;
        this.fromAddress = fromAddress;
        this.rateLimiter = rateLimiter;
        this.operationTimeout = operationTimeout;
    }

    @Override
    public NotificationResult sendNotification(Recipient recipient,
                                               NotificationContent notificationContent,
                                               NotificationData data) {
        log.info("Sending {} notification via Azure Communication Services to recipient: {} for document: {}",
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
                log.info("Attempt {}/{} - Sending email via ACS to: {} for document: {}",
                        attempt, MAX_RETRIES, recipient.getTarget(), data.getDocumentId());

                EmailMessage message = buildEmailMessage(recipient, notificationContent, data);
                SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
                PollResponse<EmailSendResult> response = poller.waitForCompletion(operationTimeout);

                EmailSendResult result = response.getValue();
                if (result == null || result.getStatus() != EmailSendStatus.SUCCEEDED) {
                    throw new IllegalStateException("ACS email operation finished with status: "
                            + (result != null ? result.getStatus() : "TIMED_OUT after " + operationTimeout));
                }

                log.info("Email sent successfully via ACS to: {} | OperationId: {} | Attempt: {}",
                        recipient.getTarget(), result.getId(), attempt);

                return new NotificationResult(
                        true, result.getId(), NotificationChannel.EMAIL,
                        recipient.getTarget(),
                        "Email delivered successfully to " + recipient.getTarget()
                );

            } catch (NotificationDomainException e) {
                throw e;
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for document: {} - Error: {}",
                        attempt, MAX_RETRIES, data.getDocumentId(), e.getMessage());

                if (attempt < MAX_RETRIES) {
                    sleepWithBackoff(attempt);
                }
            }
        }

        log.error("All {} attempts failed to send email via ACS to: {} for document: {}",
                MAX_RETRIES, recipient.getTarget(), data.getDocumentId(), lastException);
        throw new NotificationDomainException(
                "Failed to send email to " + recipient.getTarget() + " after " + MAX_RETRIES
                        + " attempts: " + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    private EmailMessage buildEmailMessage(Recipient recipient,
                                           NotificationContent notificationContent,
                                           NotificationData data) {
        EmailMessage message = new EmailMessage()
                .setSenderAddress(fromAddress)
                .setToRecipients(recipient.getTarget())
                .setSubject(notificationContent.getSubject())
                .setBodyHtml(EmailContentComposer.buildHtmlBody(notificationContent, data));

        boolean hasAttachment = notificationContent.getContentBase64() != null
                && notificationContent.getFileName() != null;
        if (hasAttachment) {
            byte[] decodedContent = Base64.getDecoder().decode(notificationContent.getContentBase64());
            String mimeType = EmailContentComposer.resolveAttachmentMimeType(notificationContent.getContentType());
            message.setAttachments(List.of(new EmailAttachment(
                    notificationContent.getFileName(), mimeType, BinaryData.fromBytes(decodedContent))));
        }

        return message;
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
}
