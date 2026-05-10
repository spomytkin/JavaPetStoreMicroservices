package com.PetStore.product.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {
    
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testCategoryBuilder() {
        // Given
        String id = "cat-001";
        String name = "Cats";
        String description = "Products for cats and kittens";
        String parentId = null;
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");
        LocalDateTime now = LocalDateTime.now();

        // When
        Category category = Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .parentId(parentId)
                .childIds(childIds)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertEquals(id, category.getId());
        assertEquals(name, category.getName());
        assertEquals(description, category.getDescription());
        assertNull(category.getParentId());
        assertEquals(childIds, category.getChildIds());
        assertEquals(now, category.getCreatedAt());
        assertEquals(now, category.getUpdatedAt());
    }

    @Test
    void testCategoryNoArgsConstructor() {
        // When
        Category category = new Category();

        // Then
        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getDescription());
        assertNull(category.getParentId());
        assertNull(category.getChildIds());
        assertNull(category.getCreatedAt());
        assertNull(category.getUpdatedAt());
    }

    @Test
    void testCategoryAllArgsConstructor() {
        // Given
        String id = "cat-001";
        String name = "Cats";
        String description = "Products for cats and kittens";
        String parentId = "pets-001";
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        // When
        Category category = new Category(id, name, description, parentId, childIds, createdAt, updatedAt);

        // Then
        assertEquals(id, category.getId());
        assertEquals(name, category.getName());
        assertEquals(description, category.getDescription());
        assertEquals(parentId, category.getParentId());
        assertEquals(childIds, category.getChildIds());
        assertEquals(createdAt, category.getCreatedAt());
        assertEquals(updatedAt, category.getUpdatedAt());
    }

    @Test
    void testCategorySettersAndGetters() {
        // Given
        Category category = new Category();
        String id = "cat-001";
        String name = "Cats";
        String description = "Products for cats and kittens";
        String parentId = "pets-001";
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        // When
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        category.setParentId(parentId);
        category.setChildIds(childIds);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(updatedAt);

        // Then
        assertEquals(id, category.getId());
        assertEquals(name, category.getName());
        assertEquals(description, category.getDescription());
        assertEquals(parentId, category.getParentId());
        assertEquals(childIds, category.getChildIds());
        assertEquals(createdAt, category.getCreatedAt());
        assertEquals(updatedAt, category.getUpdatedAt());
    }

    @Test
    void testCategoryEqualsAndHashCode() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");
        
        Category category1 = Category.builder()
                .id("cat-001")
                .name("Cats")
                .description("Products for cats and kittens")
                .parentId(null)
                .childIds(childIds)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Category category2 = Category.builder()
                .id("cat-001")
                .name("Cats")
                .description("Products for cats and kittens")
                .parentId(null)
                .childIds(childIds)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Category category3 = Category.builder()
                .id("dog-001")
                .name("Dogs")
                .description("Products for dogs")
                .parentId(null)
                .childIds(Arrays.asList("dog-food-001"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertEquals(category1, category2);
        assertEquals(category1.hashCode(), category2.hashCode());
        assertNotEquals(category1, category3);
        assertNotEquals(category1.hashCode(), category3.hashCode());
    }

    @Test
    void testCategoryToString() {
        // Given
        Category category = Category.builder()
                .id("cat-001")
                .name("Cats")
                .description("Products for cats and kittens")
                .parentId(null)
                .childIds(Arrays.asList("cat-food-001", "cat-toys-001"))
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();

        // When
        String toString = category.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("cat-001"));
        assertTrue(toString.contains("Cats"));
        assertTrue(toString.contains("Products for cats and kittens"));
    }
    @Test

    void testValidCategory() {
        // Given
        Category category = Category.builder()
                .name("Valid Category")
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidCategoryNameBlank() {
        // Given
        Category category = Category.builder()
                .name("")
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name is required", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryNameTooShort() {
        // Given
        Category category = Category.builder()
                .name("A")
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name must be between 2 and 100 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryNameTooLong() {
        // Given
        String longName = "A".repeat(101);
        Category category = Category.builder()
                .name(longName)
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Category name must be between 2 and 100 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryDescriptionTooLong() {
        // Given
        String longDescription = "A".repeat(501);
        Category category = Category.builder()
                .name("Valid Category")
                .description(longDescription)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Description cannot exceed 500 characters", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryMissingCreatedAt() {
        // Given
        Category category = Category.builder()
                .name("Valid Category")
                .description("Valid description")
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Creation timestamp is required", violations.iterator().next().getMessage());
    }

    @Test
    void testInvalidCategoryMissingUpdatedAt() {
        // Given
        Category category = Category.builder()
                .name("Valid Category")
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Update timestamp is required", violations.iterator().next().getMessage());
    }

    @Test
    void testDefaultChildIdsIsEmptyList() {
        // Given & When
        Category category = Category.builder()
                .name("Valid Category")
                .description("Valid description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Then
        assertNotNull(category.getChildIds());
        assertTrue(category.getChildIds().isEmpty());
    }
}