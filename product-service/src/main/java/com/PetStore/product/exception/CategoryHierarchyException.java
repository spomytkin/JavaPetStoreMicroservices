package com.PetStore.product.exception;

public class CategoryHierarchyException extends RuntimeException {
    public CategoryHierarchyException(String message) {
        super(message);
    }
    
    public CategoryHierarchyException(String message, Throwable cause) {
        super(message, cause);
    }
}