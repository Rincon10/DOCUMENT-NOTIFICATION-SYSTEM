package com.document.notification.system.generator.service.domain.service;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.exception.GeneratorDomainException;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
            String base64Content = switch (documentType) {
                case PDF -> generatePdfContent(documentId, customerId, data);
                case HTML -> encodeToBase64(generateHtmlContent(documentId, customerId, data));
                default -> throw new GeneratorDomainException(
                        "Unsupported document type: " + documentType);
            };

            log.info("Successfully generated {} content of size {} bytes",
                    documentType, base64Content.length());

            return new GeneratedContent(base64Content, documentType);

        } catch (Exception e) {
            log.error("Failed to generate content for document: {}", documentId, e);
            throw new GeneratorDomainException(
                    "Failed to generate document content: " + e.getMessage());
        }
    }

    private String generatePdfContent(String documentId, String customerId, GenerationContentData data) {
        try {
            byte[] pdfBytes = buildPdfBytes(documentId, customerId, data);
            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (IOException e) {
            throw new GeneratorDomainException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private byte[] buildPdfBytes(String documentId, String customerId, GenerationContentData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        // Header
        write(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

        // Object 1 - Catalog
        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Object 2 - Pages
        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        // Object 3 - Page
        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 5 0 R /Resources << /Font << /F1 4 0 R >> >> >>\nendobj\n");

        // Object 4 - Font
        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        // Build page content stream
        byte[] streamBytes = buildPageStream(documentId, customerId, data);

        // Object 5 - Content stream
        offsets.add(out.size());
        write(out, "5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
        out.write(streamBytes);
        write(out, "\nendstream\nendobj\n");

        // Cross-reference table
        int xrefOffset = out.size();
        write(out, "xref\n");
        write(out, "0 " + (offsets.size() + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format("%010d 00000 n \n", offset));
        }

        // Trailer
        write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n");
        write(out, "startxref\n" + xrefOffset + "\n%%EOF\n");

        return out.toByteArray();
    }

    private byte[] buildPageStream(String documentId, String customerId, GenerationContentData data) {
        StringBuilder stream = new StringBuilder();
        int y = 740;

        stream.append("BT\n");

        // Title
        stream.append("/F1 18 Tf\n");
        stream.append("50 ").append(y).append(" Td\n");
        stream.append("(Document Generated) Tj\n");
        y -= 40;

        // Separator
        stream.append("/F1 10 Tf\n");
        stream.append("50 ").append(y).append(" Td\n");
        stream.append("(========================================) Tj\n");
        y -= 25;

        // Document info
        stream.append("/F1 11 Tf\n");
        y = appendPdfLine(stream, "Document ID: " + documentId, y);
        y = appendPdfLine(stream, "Customer ID: " + customerId, y);
        y = appendPdfLine(stream, "Generated At: " + LocalDateTime.now().format(FORMATTER), y);
        y -= 15;

        if (data != null) {
            y = appendPdfLine(stream, "--- Additional Data ---", y);
            y -= 5;
            if (data.getRequestId() != null) {
                y = appendPdfLine(stream, "Request ID: " + data.getRequestId(), y);
            }
            if (data.getSagaId() != null) {
                y = appendPdfLine(stream, "Saga ID: " + data.getSagaId(), y);
            }
        }

        stream.append("ET\n");
        return stream.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private int appendPdfLine(StringBuilder stream, String text, int y) {
        String escaped = escapePdfString(text);
        stream.append("50 ").append(y).append(" Td\n");
        stream.append("(").append(escaped).append(") Tj\n");
        return y - 20;
    }

    private String escapePdfString(String text) {
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)");
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

    private void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    private void appendHtmlContentData(StringBuilder html, GenerationContentData data) {
        appendHtmlRow(html, "Generation ID", data.getGenerationId());
        appendHtmlRow(html, "Document ID", data.getDocumentId());
        appendHtmlRow(html, "Customer ID", data.getCustomerId());
        appendHtmlRow(html, "Request ID", data.getRequestId());
        appendHtmlRow(html, "Saga ID", data.getSagaId());
    }

    private void appendHtmlRow(StringBuilder html, String label, String value) {
        if (value != null) {
            html.append("            <tr><td><strong>").append(label)
                    .append(":</strong></td><td>").append(value).append("</td></tr>\n");
        }
    }
}

