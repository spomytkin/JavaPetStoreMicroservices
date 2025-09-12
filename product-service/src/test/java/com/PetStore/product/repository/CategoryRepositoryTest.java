package com.PetStore.product.repository;

import com.PetStore.product.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class CategoryRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private CategoryRepository categoryRepository;

    private Category rootCategory;
    private Category childCategory1;
    private Category childCategory2;
    private Category grandChildCategory;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        // Create root category
        rootCategory = Category.builder()
                .id("root-001")
                .name("Pets")
                .description("All pet products")
                .parentId(null)
                .childIds(Arrays.asList("child-001", "child-002"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Create child categories
        childCategory1 = Category.builder()
                .id("child-001")
                .name("Cats")
                .description("Products for cats")
                .parentId("root-001")
                .childIds(Arrays.asList("grandchild-001"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        childCategory2 = Category.builder()
                .id("child-002")
                .name("Dogs")
                .description("Products for dogs")
                .parentId("root-001")
                .childIds(Arrays.asList())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Create grandchild category
        grandChildCategory = Category.builder()
                .id("grandchild-001")
                .name("Cat Food")
                .description("Food for cats")
                .parentId("child-001")
                .childIds(Arrays.asList())
                .createdAt(now)
                .updatedAt(now)
                .build();

        categoryRepository.saveAll(Arrays.asList(rootCategory, childCategory1, childCategory2, grandChildCategory));
    }

    @Test
    void testSaveAndFindById() {
        // Given
        Category newCategory = Category.builder()
                .name("Birds")
                .description("Products for birds")
                .parentId("root-001")
                .childIds(Arrays.asList())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Category savedCategory = categoryRepository.save(newCategory);
        Optional<Category> foundCategory = categoryRepository.findById(savedCategory.getId());

        // Then
        assertTrue(foundCategory.isPresent());
        assertEquals("Birds", foundCategory.get().getName());
        assertEquals("Products for birds", foundCategory.get().getDescription());
        assertEquals("root-001", foundCategory.get().getParentId());
    }

    @Test
    void testFindByParentId() {
        // When
        List<Category> childCategories = categoryRepository.findByParentId("root-001");

        // Then
        assertEquals(2, childCategories.size());
        assertTrue(childCategories.stream().anyMatch(cat -> "Cats".equals(cat.getName())));
        assertTrue(childCategories.stream().anyMatch(cat -> "Dogs".equals(cat.getName())));
    }

    @Test
    void testFindByParentIdIsNull() {
        // When
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();

        // Then
        assertEquals(1, rootCategories.size());
        assertEquals("Pets", rootCategories.get(0).getName());
        assertNull(rootCategories.get(0).getParentId());
    }

    @Test
    void testExistsByParentId() {
        // When & Then
        assertTrue(categoryRepository.existsByParentId("root-001"));
        assertTrue(categoryRepository.existsByParentId("child-001"));
        assertFalse(categoryRepository.existsByParentId("child-002"));
        assertFalse(categoryRepository.existsByParentId("nonexistent-id"));
    }

    @Test
    void testFindByNameAndParentId() {
        // When
        List<Category> categories = categoryRepository.findByNameAndParentId("Cats", "root-001");

        // Then
        assertEquals(1, categories.size());
        assertEquals("Cats", categories.get(0).getName());
        assertEquals("root-001", categories.get(0).getParentId());
    }

    @Test
    void testFindByNameAndParentIdIsNull() {
        // When
        List<Category> categories = categoryRepository.findByNameAndParentIdIsNull("Pets");

        // Then
        assertEquals(1, categories.size());
        assertEquals("Pets", categories.get(0).getName());
        assertNull(categories.get(0).getParentId());
    }

    @Test
    void testFindByNameAndParentIdNotFound() {
        // When
        List<Category> categories = categoryRepository.findByNameAndParentId("NonExistent", "root-001");

        // Then
        assertTrue(categories.isEmpty());
    }

    @Test
    void testDeleteCategory() {
        // Given
        String categoryId = childCategory2.getId();

        // When
        categoryRepository.deleteById(categoryId);
        Optional<Category> deletedCategory = categoryRepository.findById(categoryId);

        // Then
        assertFalse(deletedCategory.isPresent());
    }

    @Test
    void testFindAll() {
        // When
        List<Category> allCategories = categoryRepository.findAll();

        // Then
        assertEquals(4, allCategories.size());
    }

    @Test
    void testUpdateCategory() {
        // Given
        String categoryId = childCategory1.getId();
        String newDescription = "Updated description for cats";

        // When
        Optional<Category> categoryOptional = categoryRepository.findById(categoryId);
        assertTrue(categoryOptional.isPresent());
        
        Category category = categoryOptional.get();
        category.setDescription(newDescription);
        category.setUpdatedAt(LocalDateTime.now());
        
        Category updatedCategory = categoryRepository.save(category);

        // Then
        assertEquals(newDescription, updatedCategory.getDescription());
        assertNotEquals(category.getCreatedAt(), updatedCategory.getUpdatedAt());
    }

    @Test
    void testCascadeQueryWithMultipleLevels() {
        // When - Find all children of root category
        List<Category> directChildren = categoryRepository.findByParentId("root-001");
        
        // Then
        assertEquals(2, directChildren.size());
        
        // When - Find grandchildren
        List<Category> grandChildren = categoryRepository.findByParentId("child-001");
        
        // Then
        assertEquals(1, grandChildren.size());
        assertEquals("Cat Food", grandChildren.get(0).getName());
    }
 
   @Test
    void testExistsByNameAndParentId() {
        // When & Then
        assertTrue(categoryRepository.existsByNameAndParentId("Cats", "root-001"));
        assertTrue(categoryRepository.existsByNameAndParentId("Dogs", "root-001"));
        assertFalse(categoryRepository.existsByNameAndParentId("Birds", "root-001"));
        assertFalse(categoryRepository.existsByNameAndParentId("Cats", "nonexistent-id"));
    }

    @Test
    void testExistsByNameAndParentIdIsNull() {
        // When & Then
        assertTrue(categoryRepository.existsByNameAndParentIdIsNull("Pets"));
        assertFalse(categoryRepository.existsByNameAndParentIdIsNull("NonExistent"));
    }
}