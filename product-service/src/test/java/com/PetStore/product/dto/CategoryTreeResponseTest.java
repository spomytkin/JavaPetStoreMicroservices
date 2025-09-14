package com.PetStore.product.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTreeResponseTest {

    @Test
    void testCategoryTreeResponseRecordProperties() {
        // Given
        String id = "cat-001";
        String name = "Cats";
        String description = "Products for cats and kittens";
        String parentId = "pets-001";
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 12, 0);
        
        CategoryTreeResponse child1 = new CategoryTreeResponse(
            "cat-food-001", "Cat Food", "Food for cats", "cat-001", 
            Arrays.asList(), createdAt, updatedAt
        );
        CategoryTreeResponse child2 = new CategoryTreeResponse(
            "cat-toys-001", "Cat Toys", "Toys for cats", "cat-001", 
            Arrays.asList(), createdAt, updatedAt
        );
        List<CategoryTreeResponse> children = Arrays.asList(child1, child2);

        // When
        CategoryTreeResponse response = new CategoryTreeResponse(
            id, name, description, parentId, children, createdAt, updatedAt
        );

        // Then
        assertEquals(id, response.id());
        assertEquals(name, response.name());
        assertEquals(description, response.description());
        assertEquals(parentId, response.parentId());
        assertEquals(children, response.children());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void testCategoryTreeResponseWithNullValues() {
        // Given & When
        CategoryTreeResponse response = new CategoryTreeResponse(
            "cat-001", "Cats", null, null, null, 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("cat-001", response.id());
        assertEquals("Cats", response.name());
        assertNull(response.description());
        assertNull(response.parentId());
        assertNull(response.children());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void testCategoryTreeResponseEquality() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 12, 0);
        
        CategoryTreeResponse child = new CategoryTreeResponse(
            "cat-food-001", "Cat Food", "Food for cats", "cat-001", 
            Arrays.asList(), createdAt, updatedAt
        );
        List<CategoryTreeResponse> children = Arrays.asList(child);

        CategoryTreeResponse response1 = new CategoryTreeResponse(
            "cat-001", "Cats", "Description", "pets-001", children, createdAt, updatedAt
        );
        CategoryTreeResponse response2 = new CategoryTreeResponse(
            "cat-001", "Cats", "Description", "pets-001", children, createdAt, updatedAt
        );
        CategoryTreeResponse response3 = new CategoryTreeResponse(
            "dog-001", "Dogs", "Description", "pets-001", Arrays.asList(), createdAt, updatedAt
        );

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1, response3);
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testCategoryTreeResponseToString() {
        // Given
        CategoryTreeResponse child = new CategoryTreeResponse(
            "cat-food-001", "Cat Food", "Food for cats", "cat-001", 
            Arrays.asList(), LocalDateTime.now(), LocalDateTime.now()
        );
        
        CategoryTreeResponse response = new CategoryTreeResponse(
            "cat-001", "Cats", "Products for cats", "pets-001", 
            Arrays.asList(child), 
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
    void testCategoryTreeResponseWithEmptyChildren() {
        // Given & When
        CategoryTreeResponse response = new CategoryTreeResponse(
            "cat-001", "Cats", "Description", null, 
            Arrays.asList(), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("cat-001", response.id());
        assertEquals("Cats", response.name());
        assertNotNull(response.children());
        assertTrue(response.children().isEmpty());
    }

    @Test
    void testCategoryTreeResponseRootCategory() {
        // Given
        CategoryTreeResponse catChild = new CategoryTreeResponse(
            "cats-001", "Cats", "Cat products", "pets-001", 
            Arrays.asList(), LocalDateTime.now(), LocalDateTime.now()
        );
        CategoryTreeResponse dogChild = new CategoryTreeResponse(
            "dogs-001", "Dogs", "Dog products", "pets-001", 
            Arrays.asList(), LocalDateTime.now(), LocalDateTime.now()
        );
        
        // When (root category has no parent)
        CategoryTreeResponse response = new CategoryTreeResponse(
            "pets-001", "Pets", "All pet products", null, 
            Arrays.asList(catChild, dogChild), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("pets-001", response.id());
        assertEquals("Pets", response.name());
        assertNull(response.parentId());
        assertEquals(2, response.children().size());
        assertEquals("cats-001", response.children().get(0).id());
        assertEquals("dogs-001", response.children().get(1).id());
    }

    @Test
    void testCategoryTreeResponseLeafCategory() {
        // Given & When (leaf category has no children)
        CategoryTreeResponse response = new CategoryTreeResponse(
            "dry-food-001", "Dry Food", "Dry pet food products", "cat-food-001", 
            Arrays.asList(), 
            LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("dry-food-001", response.id());
        assertEquals("Dry Food", response.name());
        assertEquals("cat-food-001", response.parentId());
        assertNotNull(response.children());
        assertTrue(response.children().isEmpty());
    }

    @Test
    void testCategoryTreeResponseNestedHierarchy() {
        // Given - Create a 3-level hierarchy
        CategoryTreeResponse grandChild = new CategoryTreeResponse(
            "premium-dry-food-001", "Premium Dry Food", "Premium dry cat food", "dry-food-001", 
            Arrays.asList(), LocalDateTime.now(), LocalDateTime.now()
        );
        
        CategoryTreeResponse child = new CategoryTreeResponse(
            "dry-food-001", "Dry Food", "Dry cat food", "cat-food-001", 
            Arrays.asList(grandChild), LocalDateTime.now(), LocalDateTime.now()
        );
        
        // When
        CategoryTreeResponse parent = new CategoryTreeResponse(
            "cat-food-001", "Cat Food", "All cat food products", "cats-001", 
            Arrays.asList(child), LocalDateTime.now(), LocalDateTime.now()
        );

        // Then
        assertEquals("cat-food-001", parent.id());
        assertEquals(1, parent.children().size());
        assertEquals("dry-food-001", parent.children().get(0).id());
        assertEquals(1, parent.children().get(0).children().size());
        assertEquals("premium-dry-food-001", parent.children().get(0).children().get(0).id());
        assertTrue(parent.children().get(0).children().get(0).children().isEmpty());
    }
}