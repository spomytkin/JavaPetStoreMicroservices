package com.PetStore.product.exception;

public class CategoryDeletionException extends RuntimeException {
    public CategoryDeletionException(String message) {
        super(message);
    }
    
    public CategoryDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}