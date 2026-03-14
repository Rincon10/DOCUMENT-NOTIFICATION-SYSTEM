package com.document.notification.system.notification.service.domain.valueobject;

import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import org.apache.commons.lang3.StringUtils;

/**
 * Value object representing the result of a notification delivery attempt.
 * Analogous to GeneratedContent in generator-service, but for email sending.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
public class NotificationResult {
    private final boolean successful;
    private final String messageId;
    private final NotificationChannel channel;
    private final String recipientAddress;
    private final String details;

    public NotificationResult(boolean successful,
                               String messageId,
                               NotificationChannel channel,
                               String recipientAddress,
                               String details) {
        this.successful = successful;
        this.messageId = messageId;
        this.channel = channel;
        this.recipientAddress = recipientAddress;
        this.details = details;
        validate();
    }

    private void validate() {
        if (channel == null) {
            throw new NotificationDomainException("Notification channel cannot be null");
        }
        if (StringUtils.isBlank(recipientAddress)) {
            throw new NotificationDomainException("Recipient address cannot be empty");
        }
        if (successful && StringUtils.isBlank(messageId)) {
            throw new NotificationDomainException("Message ID is required for successful notifications");
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessageId() {
        return messageId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationResult that = (NotificationResult) o;
        return messageId != null && messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}
