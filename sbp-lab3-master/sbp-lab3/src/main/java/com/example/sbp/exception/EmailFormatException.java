package com.example.sbp.exception;

public class EmailFormatException extends RuntimeException {
    public EmailFormatException(String message) {
        super(message);
    }
}