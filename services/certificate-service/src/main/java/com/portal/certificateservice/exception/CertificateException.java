package com.portal.certificateservice.exception;

import org.springframework.http.HttpStatus;

public class CertificateException extends RuntimeException {
    private final HttpStatus status;

    public CertificateException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public CertificateException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
