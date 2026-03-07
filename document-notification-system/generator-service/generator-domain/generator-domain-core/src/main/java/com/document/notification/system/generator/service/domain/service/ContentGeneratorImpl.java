package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.exception.GeneratorDomainException;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Content generator implementation for creating documents
 * Currently generates simple text-based content encoded in Base64
 * TODO: Implement proper PDF generation using Apache PDFBox or iText when needed
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
@Slf4j
public class ContentGeneratorImpl implements IContentGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public GeneratedContent generateContent(DocumentType documentType,
                                            String documentId,
                                            String customerId,
                                            GenerationContentData data) {
        log.info("Generating {} content for document: {} and customer: {}",
                documentType, documentId, customerId);

        try {
            String content = switch (documentType) {
                case PDF -> generatePdfContent(documentId, customerId, data);
                case HTML -> generateHtmlContent(documentId, customerId, data);
                default -> throw new GeneratorDomainException(
                        "Unsupported document type: " + documentType);
            };

            String base64Content = encodeToBase64(content);
            log.info("Successfully generated {} content of size {} bytes",
                    documentType, base64Content.length());

            return new GeneratedContent(base64Content, documentType);

        } catch (Exception e) {
            log.error("Failed to generate content for document: {}", documentId, e);
            throw new GeneratorDomainException(
                    "Failed to generate document content: " + e.getMessage());
        }
    }

    // TO DO: Handle better using factory or strategy pattern for different document types
    private String generatePdfContent(String documentId, String customerId, GenerationContentData data) {
        // Simple text content that simulates PDF structure
        // In production, use Apache PDFBox or iText for real PDF generation
        StringBuilder content = new StringBuilder();
        content.append("%PDF-1.4\n");
        content.append("% Simulated PDF Document\n");
        content.append("1 0 obj\n");
        content.append("<< /Type /Catalog /Pages 2 0 R >>\n");
        content.append("endobj\n\n");
        content.append("2 0 obj\n");
        content.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
        content.append("endobj\n\n");
        content.append("3 0 obj\n");
        content.append("<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>\n");
        content.append("endobj\n\n");
        content.append("4 0 obj\n");
        content.append("<< /Length 100 >>\n");
        content.append("stream\n");
        content.append("DOCUMENT GENERATED\n");
        content.append("===================\n\n");
        content.append("Document ID: ").append(documentId).append("\n");
        content.append("Customer ID: ").append(customerId).append("\n");
        content.append("Generated At: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        if (data != null) {
            content.append("Additional Data:\n");
            appendContentData(content, data);
        }

        content.append("\nendstream\n");
        content.append("endobj\n");
        content.append("%%EOF\n");

        return content.toString();
    }

    private String generateHtmlContent(String documentId, String customerId, GenerationContentData data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Document ").append(documentId).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 40px; }\n");
        html.append("        .header { background-color: #f0f0f0; padding: 20px; }\n");
        html.append("        .content { margin-top: 20px; }\n");
        html.append("        .data-table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("        .data-table td { border: 1px solid #ddd; padding: 8px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>Document Generated</h1>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"content\">\n");
        html.append("        <h2>Document Information</h2>\n");
        html.append("        <table class=\"data-table\">\n");
        html.append("            <tr><td><strong>Document ID:</strong></td><td>")
                .append(documentId).append("</td></tr>\n");
        html.append("            <tr><td><strong>Customer ID:</strong></td><td>")
                .append(customerId).append("</td></tr>\n");
        html.append("            <tr><td><strong>Generated At:</strong></td><td>")
                .append(LocalDateTime.now().format(FORMATTER)).append("</td></tr>\n");

        if (data != null) {
            appendHtmlContentData(html, data);
        }

        html.append("        </table>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String encodeToBase64(String content) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(contentBytes);
    }

    private void appendContentData(StringBuilder content, GenerationContentData data) {
        appendLine(content, "Generation ID", data.getGenerationId());
        appendLine(content, "Document ID", data.getDocumentId());
        appendLine(content, "Customer ID", data.getCustomerId());
        appendLine(content, "File Extension", data.getFileExtension());
        appendLine(content, "Request ID", data.getRequestId());
        appendLine(content, "Saga ID", data.getSagaId());
    }

    private void appendHtmlContentData(StringBuilder html, GenerationContentData data) {
        appendHtmlRow(html, "Generation ID", data.getGenerationId());
        appendHtmlRow(html, "Document ID", data.getDocumentId());
        appendHtmlRow(html, "Customer ID", data.getCustomerId());
        appendHtmlRow(html, "File Extension", data.getFileExtension());
        appendHtmlRow(html, "Request ID", data.getRequestId());
        appendHtmlRow(html, "Saga ID", data.getSagaId());
    }

    private void appendLine(StringBuilder content, String label, String value) {
        if (value != null) {
            content.append("  ").append(label).append(": ").append(value).append("\n");
        }
    }

    private void appendHtmlRow(StringBuilder html, String label, String value) {
        if (value != null) {
            html.append("            <tr><td><strong>").append(label)
                    .append(":</strong></td><td>").append(value).append("</td></tr>\n");
        }
    }
}

