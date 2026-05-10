package com.PetStore.product.repository;

import com.PetStore.product.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ProductRepository category-based query methods.
 * These tests require Docker to be available for TestContainers.
 * 
 * To run these tests:
 * 1. Ensure Docker is running
 * 2. Run: mvn test -Dtest=ProductRepositoryIntegrationTest -pl product-service
 * 
 * These tests verify the actual MongoDB query behavior for:
 * - findByCategoryIdsContaining: finds products that contain a specific category ID
 * - findByCategoryIdsIn: finds products that contain any of the specified category IDs
 */
@DataMongoTest
@Testcontainers
class ProductRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProductRepository productRepository;

    private Product catFood;
    private Product dogFood;
    private Product catToy;
    private Product dogToy;
    private Product multiCategoryProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        // Create products with different category assignments
        catFood = Product.builder()
                .id("product-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(Arrays.asList("cats", "cat-food"))
                .build();

        dogFood = Product.builder()
                .id("product-002")
                .name("Premium Dog Food")
                .description("High-quality dry food for adult dogs")
                .skuCode("DF-001")
                .price(new BigDecimal("29.99"))
                .categoryIds(Arrays.asList("dogs", "dog-food"))
                .build();

        catToy = Product.builder()
                .id("product-003")
                .name("Interactive Cat Toy")
                .description("Electronic toy for cats")
                .skuCode("CT-001")
                .price(new BigDecimal("15.99"))
                .categoryIds(Arrays.asList("cats", "cat-toys"))
                .build();

        dogToy = Product.builder()
                .id("product-004")
                .name("Chew Toy for Dogs")
                .description("Durable chew toy for large dogs")
                .skuCode("DT-001")
                .price(new BigDecimal("12.99"))
                .categoryIds(Arrays.asList("dogs", "dog-toys"))
                .build();

        multiCategoryProduct = Product.builder()
                .id("product-005")
                .name("Universal Pet Bed")
                .description("Comfortable bed suitable for cats and small dogs")
                .skuCode("PB-001")
                .price(new BigDecimal("39.99"))
                .categoryIds(Arrays.asList("cats", "dogs", "pet-furniture"))
                .build();

        productRepository.saveAll(Arrays.asList(catFood, dogFood, catToy, dogToy, multiCategoryProduct));
    }

    @Test
    void testFindByCategoryIdsContaining_SingleCategory() {
        // When
        List<Product> catsProducts = productRepository.findByCategoryIdsContaining("cats");

        // Then
        assertEquals(3, catsProducts.size());
        assertTrue(catsProducts.stream().anyMatch(p -> "Premium Cat Food".equals(p.getName())));
        assertTrue(catsProducts.stream().anyMatch(p -> "Interactive Cat Toy".equals(p.getName())));
        assertTrue(catsProducts.stream().anyMatch(p -> "Universal Pet Bed".equals(p.getName())));
    }

    @Test
    void testFindByCategoryIdsContaining_SpecificSubcategory() {
        // When
        List<Product> catFoodProducts = productRepository.findByCategoryIdsContaining("cat-food");

        // Then
        assertEquals(1, catFoodProducts.size());
        assertEquals("Premium Cat Food", catFoodProducts.get(0).getName());
        assertEquals("CF-001", catFoodProducts.get(0).getSkuCode());
    }

    @Test
    void testFindByCategoryIdsContaining_NoResults() {
        // When
        List<Product> birdsProducts = productRepository.findByCategoryIdsContaining("birds");

        // Then
        assertTrue(birdsProducts.isEmpty());
    }

    @Test
    void testFindByCategoryIdsContaining_NonExistentCategory() {
        // When
        List<Product> nonExistentProducts = productRepository.findByCategoryIdsContaining("non-existent");

        // Then
        assertTrue(nonExistentProducts.isEmpty());
    }

    @Test
    void testFindByCategoryIdsIn_MultipleCategories() {
        // When
        List<Product> foodProducts = productRepository.findByCategoryIdsIn(Arrays.asList("cat-food", "dog-food"));

        // Then
        assertEquals(2, foodProducts.size());
        assertTrue(foodProducts.stream().anyMatch(p -> "Premium Cat Food".equals(p.getName())));
        assertTrue(foodProducts.stream().anyMatch(p -> "Premium Dog Food".equals(p.getName())));
    }

    @Test
    void testFindByCategoryIdsIn_SingleCategoryInList() {
        // When
        List<Product> toyProducts = productRepository.findByCategoryIdsIn(Arrays.asList("cat-toys"));

        // Then
        assertEquals(1, toyProducts.size());
        assertEquals("Interactive Cat Toy", toyProducts.get(0).getName());
    }

    @Test
    void testFindByCategoryIdsIn_MixedExistingAndNonExisting() {
        // When
        List<Product> mixedProducts = productRepository.findByCategoryIdsIn(Arrays.asList("cats", "birds", "fish"));

        // Then
        assertEquals(3, mixedProducts.size());
        assertTrue(mixedProducts.stream().anyMatch(p -> "Premium Cat Food".equals(p.getName())));
        assertTrue(mixedProducts.stream().anyMatch(p -> "Interactive Cat Toy".equals(p.getName())));
        assertTrue(mixedProducts.stream().anyMatch(p -> "Universal Pet Bed".equals(p.getName())));
    }

    @Test
    void testFindByCategoryIdsIn_EmptyList() {
        // When
        List<Product> emptyResults = productRepository.findByCategoryIdsIn(Arrays.asList());

        // Then
        assertTrue(emptyResults.isEmpty());
    }

    @Test
    void testFindByCategoryIdsIn_AllNonExistentCategories() {
        // When
        List<Product> nonExistentResults = productRepository.findByCategoryIdsIn(Arrays.asList("birds", "fish", "reptiles"));

        // Then
        assertTrue(nonExistentResults.isEmpty());
    }

    @Test
    void testFindByCategoryIdsIn_OverlappingResults() {
        // When - Search for categories that should return overlapping products
        List<Product> overlappingResults = productRepository.findByCategoryIdsIn(Arrays.asList("cats", "dogs"));

        // Then - Should return all products that belong to either cats or dogs (including multi-category product)
        assertEquals(5, overlappingResults.size());
        assertTrue(overlappingResults.stream().anyMatch(p -> "Premium Cat Food".equals(p.getName())));
        assertTrue(overlappingResults.stream().anyMatch(p -> "Premium Dog Food".equals(p.getName())));
        assertTrue(overlappingResults.stream().anyMatch(p -> "Interactive Cat Toy".equals(p.getName())));
        assertTrue(overlappingResults.stream().anyMatch(p -> "Chew Toy for Dogs".equals(p.getName())));
        assertTrue(overlappingResults.stream().anyMatch(p -> "Universal Pet Bed".equals(p.getName())));
    }

    @Test
    void testProductWithMultipleCategories() {
        // When
        List<Product> petFurnitureProducts = productRepository.findByCategoryIdsContaining("pet-furniture");

        // Then
        assertEquals(1, petFurnitureProducts.size());
        assertEquals("Universal Pet Bed", petFurnitureProducts.get(0).getName());
        assertEquals(Arrays.asList("cats", "dogs", "pet-furniture"), petFurnitureProducts.get(0).getCategoryIds());
    }

    @Test
    void testCombinedQueryScenarios() {
        // Test scenario: Find all cat products using findByCategoryIdsContaining
        List<Product> catProducts = productRepository.findByCategoryIdsContaining("cats");
        assertEquals(3, catProducts.size());

        // Test scenario: Find all food products using findByCategoryIdsIn
        List<Product> foodProducts = productRepository.findByCategoryIdsIn(Arrays.asList("cat-food", "dog-food"));
        assertEquals(2, foodProducts.size());

        // Test scenario: Find products in specific subcategories
        List<Product> toyProducts = productRepository.findByCategoryIdsIn(Arrays.asList("cat-toys", "dog-toys"));
        assertEquals(2, toyProducts.size());

        // Verify no overlap between food and toy queries
        assertTrue(foodProducts.stream().noneMatch(toyProducts::contains));
    }

    @Test
    void testQueryPerformanceWithLargeDataset() {
        // Create additional test products to verify query performance
        for (int i = 6; i <= 100; i++) {
            Product product = Product.builder()
                    .id("product-" + String.format("%03d", i))
                    .name("Test Product " + i)
                    .description("Test product for performance testing")
                    .skuCode("TEST-" + String.format("%03d", i))
                    .price(new BigDecimal("10.00"))
                    .categoryIds(Arrays.asList("test-category", "category-" + (i % 10)))
                    .build();
            productRepository.save(product);
        }

        // Test findByCategoryIdsContaining with larger dataset
        List<Product> testCategoryProducts = productRepository.findByCategoryIdsContaining("test-category");
        assertEquals(95, testCategoryProducts.size()); // 95 new products + 0 existing with test-category

        // Test findByCategoryIdsIn with multiple categories
        List<Product> multiCategoryResults = productRepository.findByCategoryIdsIn(
                Arrays.asList("category-1", "category-2", "category-3"));
        assertTrue(multiCategoryResults.size() >= 3); // At least 3 products should match
    }
}