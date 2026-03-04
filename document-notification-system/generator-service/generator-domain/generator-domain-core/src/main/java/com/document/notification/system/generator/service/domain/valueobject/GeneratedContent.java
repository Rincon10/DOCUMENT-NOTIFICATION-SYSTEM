package com.document.notification.system.generator.service.domain.valueobject;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.exception.GeneratorDomainException;
import org.apache.commons.lang3.StringUtils;

/**
 * Value object representing generated content in Base64 format
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
public class GeneratedContent {
    private final String base64Content;
    private final DocumentType contentType;
    private final long sizeInBytes;

    public GeneratedContent(String base64Content, DocumentType contentType) {
        this.base64Content = base64Content;
        this.contentType = contentType;
        this.sizeInBytes = calculateSize(base64Content);
        validate();
    }

    private long calculateSize(String base64) {
        if (StringUtils.isBlank(base64)) {
            return 0;
        }
        // Base64 encoding increases size by ~33%, calculate original size
        return (base64.length() * 3L) / 4L;
    }

    private void validate() {
        if (StringUtils.isBlank(base64Content)) {
            throw new GeneratorDomainException("Generated content cannot be empty");
        }
        if (contentType == null) {
            throw new GeneratorDomainException("Content type cannot be null");
        }
        // Validate Base64 format (basic check)
        if (!isValidBase64(base64Content)) {
            throw new GeneratorDomainException("Invalid Base64 content format");
        }
    }

    private boolean isValidBase64(String content) {
        // Basic validation: Base64 uses A-Z, a-z, 0-9, +, /, = characters
        return content.matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    public String getBase64Content() {
        return base64Content;
    }

    public DocumentType getContentType() {
        return contentType;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getFileExtension() {
        return contentType.name().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratedContent that = (GeneratedContent) o;
        return base64Content.equals(that.base64Content);
    }

    @Override
    public int hashCode() {
        return base64Content.hashCode();
    }
}

