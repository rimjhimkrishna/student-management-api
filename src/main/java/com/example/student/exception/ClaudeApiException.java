package com.example.student.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClaudeApiException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public ClaudeApiException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
