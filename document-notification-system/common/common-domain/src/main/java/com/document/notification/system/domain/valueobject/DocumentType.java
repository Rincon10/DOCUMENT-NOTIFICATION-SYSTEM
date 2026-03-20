package com.document.notification.system.domain.valueobject;

import java.util.Locale;

public enum DocumentType {
    PDF("application/pdf"),
    HTML("text/html"),
    XML("application/xml");

    private final String mimeType;

    DocumentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static String resolveMimeType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return null;
        }

        String normalizedDocumentType = documentType.trim();
        if (normalizedDocumentType.contains("/")) {
            return normalizedDocumentType;
        }

        try {
            return valueOf(normalizedDocumentType.toUpperCase(Locale.ROOT)).getMimeType();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
