package com.PetStore.product.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCategoryRequest() {
        // Given
        CategoryRequest request = new CategoryRequest(
                "Valid Category",
                "Valid description",
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidCategoryRequestWithoutParent() {
        // Given
        CategoryRequest request = new CategoryRequest(
                "Root Category",
                "Root category description",
                null);

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidCategoryRequestWithoutDescription() {
        // Given
        CategoryRequest request = new CategoryRequest(
                "Category Name",
                null,
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidCategoryRequestBlankName() {
        // Given
        CategoryRequest request = new CategoryRequest(
                "",
                "Valid description",
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size()); // Both @NotBlank and @Size violations
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category name is required")));
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Category name must be between 2 and 100 characters")));
    }

    @Test
    void testInvalidCategoryRequestNullName() {
        // Given
        CategoryRequest request = new CategoryRequest(
                null,
                "Valid description",
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name is required", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryRequestNameTooShort() {
        // Given
        CategoryRequest request = new CategoryRequest(
                "A",
                "Valid description",
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name must be between 2 and 100 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryRequestNameTooLong() {
        // Given
        String longName = "A".repeat(101);
        CategoryRequest request = new CategoryRequest(
                longName,
                "Valid description",
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name must be between 2 and 100 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryRequestDescriptionTooLong() {
        // Given
        String longDescription = "A".repeat(501);
        CategoryRequest request = new CategoryRequest(
                "Valid Category",
                longDescription,
                "parent-001");

        // When
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Description cannot exceed 500 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testCategoryRequestRecordProperties() {
        // Given
        String name = "Test Category";
        String description = "Test description";
        String parentId = "parent-001";

        // When
        CategoryRequest request = new CategoryRequest(name, description, parentId);

        // Then
        assertEquals(name, request.name());
        assertEquals(description, request.description());
        assertEquals(parentId, request.parentId());
    }

    @Test
    void testCategoryRequestEquality() {
        // Given
        CategoryRequest request1 = new CategoryRequest("Category", "Description", "parent-001");
        CategoryRequest request2 = new CategoryRequest("Category", "Description", "parent-001");
        CategoryRequest request3 = new CategoryRequest("Different", "Description", "parent-001");

        // Then
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1, request3);
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testCategoryRequestToString() {
        // Given
        CategoryRequest request = new CategoryRequest("Test Category", "Test description", "parent-001");

        // When
        String toString = request.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Test Category"));
        assertTrue(toString.contains("Test description"));
        assertTrue(toString.contains("parent-001"));
    }
}