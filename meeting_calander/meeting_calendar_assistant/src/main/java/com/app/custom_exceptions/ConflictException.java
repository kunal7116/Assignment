package com.app.custom_exceptions;
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
}