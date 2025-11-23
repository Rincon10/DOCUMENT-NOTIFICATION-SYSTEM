package com.document.notification.system.application.exception.handler;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.dto.ErrorDTO;
import com.document.notification.system.exception.handler.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Slf4j
@ControllerAdvice
public class DocumentExceptionHandler extends GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = {DocumentDomainException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleException(DocumentDomainException documentDomainException) {
        log.error(documentDomainException.getMessage(), documentDomainException);
        return ErrorDTO.builder()
                .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(documentDomainException.getMessage())
                .build();
    }
}
