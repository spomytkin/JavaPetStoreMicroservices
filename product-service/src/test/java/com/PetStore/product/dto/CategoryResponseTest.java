package com.PetStore.product.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryResponseTest {

    @Test
    void testCategoryResponseRecordProperties() {
        // Given
        String id = "cat-001";
        String name = "Cats";
        String description = "Products for cats and kittens";
        String parentId = "pets-001";
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 12, 0);

        // When
        CategoryResponse response = new CategoryResponse(
            id, name, description, parentId, childIds, createdAt, updatedAt
        );

        // Then
        assertEquals(id, response.id());
        assertEquals(name, response.name());
        assertEquals(description, response.description());
        assertEquals(parentId, response.parentId());
        assertEquals(childIds, response.childIds());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void testCategoryResponseWithNullValues() {
        // Given & When
        CategoryResponse response = new CategoryResponse(
            "cat-001", "Cats", null, null, null, 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("cat-001", response.id());
        assertEquals("Cats", response.name());
        assertNull(response.description());
        assertNull(response.parentId());
        assertNull(response.childIds());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void testCategoryResponseEquality() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 12, 0);
        List<String> childIds = Arrays.asList("cat-food-001", "cat-toys-001");

        CategoryResponse response1 = new CategoryResponse(
            "cat-001", "Cats", "Description", "pets-001", childIds, createdAt, updatedAt
        );
        CategoryResponse response2 = new CategoryResponse(
            "cat-001", "Cats", "Description", "pets-001", childIds, createdAt, updatedAt
        );
        CategoryResponse response3 = new CategoryResponse(
            "dog-001", "Dogs", "Description", "pets-001", Arrays.asList("dog-food-001"), createdAt, updatedAt
        );

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1, response3);
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testCategoryResponseToString() {
        // Given
        CategoryResponse response = new CategoryResponse(
            "cat-001", "Cats", "Products for cats", "pets-001", 
            Arrays.asList("cat-food-001"), 
            LocalDateTime.of(2024, 1, 1, 12, 0),
            LocalDateTime.of(2024, 1, 2, 12, 0)
        );

        // When
        String toString = response.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("cat-001"));
        assertTrue(toString.contains("Cats"));
        assertTrue(toString.contains("Products for cats"));
        assertTrue(toString.contains("pets-001"));
    }

    @Test
    void testCategoryResponseWithEmptyChildIds() {
        // Given & When
        CategoryResponse response = new CategoryResponse(
            "cat-001", "Cats", "Description", null, 
            Arrays.asList(), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("cat-001", response.id());
        assertEquals("Cats", response.name());
        assertNotNull(response.childIds());
        assertTrue(response.childIds().isEmpty());
    }

    @Test
    void testCategoryResponseRootCategory() {
        // Given & When (root category has no parent)
        CategoryResponse response = new CategoryResponse(
            "pets-001", "Pets", "All pet products", null, 
            Arrays.asList("cats-001", "dogs-001"), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("pets-001", response.id());
        assertEquals("Pets", response.name());
        assertNull(response.parentId());
        assertEquals(2, response.childIds().size());
        assertTrue(response.childIds().contains("cats-001"));
        assertTrue(response.childIds().contains("dogs-001"));
    }

    @Test
    void testCategoryResponseLeafCategory() {
        // Given & When (leaf category has no children)
        CategoryResponse response = new CategoryResponse(
            "dry-food-001", "Dry Food", "Dry pet food products", "cat-food-001", 
            Arrays.asList(), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("dry-food-001", response.id());
        assertEquals("Dry Food", response.name());
        assertEquals("cat-food-001", response.parentId());
        assertNotNull(response.childIds());
        assertTrue(response.childIds().isEmpty());
    }
}