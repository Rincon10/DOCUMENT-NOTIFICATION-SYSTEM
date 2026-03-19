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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Base64;

/**
 * Infrastructure adapter that implements the domain port INotificationSender
 * using Spring's JavaMailSender for real SMTP email delivery.
 *
 * Follows DDD: the domain defines the port (INotificationSender),
 * this adapter in the container layer provides the infrastructure implementation.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
@AllArgsConstructor
public class EmailNotificationSender implements INotificationSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;

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

        return sendEmail(recipient, notificationContent, data);
    }

    private NotificationResult sendEmail(Recipient recipient,
                                          NotificationContent notificationContent,
                                          NotificationData data) {
        try {
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

            javaMailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();
            log.info("Email sent successfully to: {} | Subject: {} | MessageId: {} | Has attachment: {}",
                    recipient.getTarget(),
                    notificationContent.getSubject(),
                    messageId,
                    hasAttachment);

            return new NotificationResult(
                    true,
                    messageId,
                    NotificationChannel.EMAIL,
                    recipient.getTarget(),
                    "Email delivered successfully to " + recipient.getTarget()
            );

        } catch (MessagingException e) {
            log.error("Failed to send email to: {} for document: {}",
                    recipient.getTarget(), data.getDocumentId(), e);
            throw new NotificationDomainException(
                    "Failed to send email to " + recipient.getTarget() + ": " + e.getMessage());
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
