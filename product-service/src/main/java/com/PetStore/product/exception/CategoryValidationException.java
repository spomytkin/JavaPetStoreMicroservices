package com.PetStore.product.exception;

public class CategoryValidationException extends RuntimeException {
    public CategoryValidationException(String message) {
        super(message);
    }
    
    public CategoryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}