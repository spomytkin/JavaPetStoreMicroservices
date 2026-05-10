package com.PetStore.product.validation;

import com.PetStore.product.dto.CategoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validCategoryRequest_ShouldPassValidation() {
        CategoryRequest validRequest = new CategoryRequest("Valid Category", "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(validRequest);
        
        assertTrue(violations.isEmpty());
    }

    @Test
    void categoryRequestWithEmptyName_ShouldFailValidation() {
        CategoryRequest invalidRequest = new CategoryRequest("", "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name is required")));
    }

    @Test
    void categoryRequestWithShortName_ShouldFailValidation() {
        CategoryRequest invalidRequest = new CategoryRequest("A", "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name must be between 2 and 100 characters")));
    }

    @Test
    void categoryRequestWithLongName_ShouldFailValidation() {
        String longName = "A".repeat(101);
        CategoryRequest invalidRequest = new CategoryRequest(longName, "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name must be between 2 and 100 characters")));
    }

    @Test
    void categoryRequestWithInvalidNameFormat_ShouldFailValidation() {
        CategoryRequest invalidRequest = new CategoryRequest("Invalid@Name!", "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name can only contain letters, numbers, spaces, hyphens, underscores, and ampersands")));
    }

    @Test
    void categoryRequestWithValidSpecialCharacters_ShouldPassValidation() {
        CategoryRequest validRequest = new CategoryRequest("Dogs & Cats-Toys_2024", "Valid description", null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(validRequest);
        
        assertTrue(violations.isEmpty());
    }

    @Test
    void categoryRequestWithLongDescription_ShouldFailValidation() {
        String longDescription = "A".repeat(501);
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", longDescription, null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Description cannot exceed 500 characters")));
    }

    @Test
    void categoryRequestWithInvalidParentIdFormat_ShouldFailValidation() {
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", "Valid description", "invalid@parent!");
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(invalidRequest);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Parent ID must be a valid identifier")));
    }

    @Test
    void categoryRequestWithValidParentId_ShouldPassValidation() {
        CategoryRequest validRequest = new CategoryRequest("Valid Name", "Valid description", "valid-parent-123");
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(validRequest);
        
        assertTrue(violations.isEmpty());
    }

    @Test
    void categoryRequestWithEmptyParentId_ShouldPassValidation() {
        CategoryRequest validRequest = new CategoryRequest("Valid Name", "Valid description", "");
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(validRequest);
        
        assertTrue(violations.isEmpty());
    }

    @Test
    void categoryRequestWithNullDescription_ShouldPassValidation() {
        CategoryRequest validRequest = new CategoryRequest("Valid Name", null, null);
        
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(validRequest);
        
        assertTrue(violations.isEmpty());
    }
}