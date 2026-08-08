package com.document.notification.system.notification.service.adapter;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;

/**
 * Builds the email body and resolves attachment metadata shared by every
 * {@link com.document.notification.system.notification.service.domain.service.INotificationSender}
 * implementation ({@link EmailNotificationSender}, {@link AzureEmailNotificationSender}),
 * so all providers deliver an identical email regardless of transport.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
public final class EmailContentComposer {

    private EmailContentComposer() {
    }

    public static String buildHtmlBody(NotificationContent notificationContent, NotificationData data) {
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

    public static String resolveAttachmentMimeType(String contentType) {
        String mimeType = DocumentType.resolveMimeType(contentType);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private static void appendRow(StringBuilder html, String label, String value) {
        if (value != null) {
            html.append("<tr><td style=\"padding: 2px 8px;\"><strong>")
                    .append(label).append(":</strong></td><td>")
                    .append(value).append("</td></tr>");
        }
    }
}
