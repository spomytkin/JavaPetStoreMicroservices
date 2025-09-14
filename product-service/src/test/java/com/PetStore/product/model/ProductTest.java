package com.PetStore.product.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testProductBuilder() {
        // Given
        String id = "prod-001";
        String name = "Premium Cat Food";
        String description = "High-quality dry food for adult cats";
        String skuCode = "CF-001";
        BigDecimal price = new BigDecimal("24.99");
        List<String> categoryIds = Arrays.asList("cats-001", "cat-food-001");

        // When
        Product product = Product.builder()
                .id(id)
                .name(name)
                .description(description)
                .skuCode(skuCode)
                .price(price)
                .categoryIds(categoryIds)
                .build();

        // Then
        assertEquals(id, product.getId());
        assertEquals(name, product.getName());
        assertEquals(description, product.getDescription());
        assertEquals(skuCode, product.getSkuCode());
        assertEquals(price, product.getPrice());
        assertEquals(categoryIds, product.getCategoryIds());
    }

    @Test
    void testProductNoArgsConstructor() {
        // When
        Product product = new Product();

        // Then
        assertNull(product.getId());
        assertNull(product.getName());
        assertNull(product.getDescription());
        assertNull(product.getSkuCode());
        assertNull(product.getPrice());
        assertNull(product.getCategoryIds());
    }

    @Test
    void testProductAllArgsConstructor() {
        // Given
        String id = "prod-001";
        String name = "Premium Cat Food";
        String description = "High-quality dry food for adult cats";
        String skuCode = "CF-001";
        BigDecimal price = new BigDecimal("24.99");
        List<String> categoryIds = Arrays.asList("cats-001", "cat-food-001");

        // When
        Product product = new Product(id, name, description, skuCode, price, categoryIds);

        // Then
        assertEquals(id, product.getId());
        assertEquals(name, product.getName());
        assertEquals(description, product.getDescription());
        assertEquals(skuCode, product.getSkuCode());
        assertEquals(price, product.getPrice());
        assertEquals(categoryIds, product.getCategoryIds());
    }

    @Test
    void testProductSettersAndGetters() {
        // Given
        Product product = new Product();
        String id = "prod-001";
        String name = "Premium Cat Food";
        String description = "High-quality dry food for adult cats";
        String skuCode = "CF-001";
        BigDecimal price = new BigDecimal("24.99");
        List<String> categoryIds = Arrays.asList("cats-001", "cat-food-001");

        // When
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setSkuCode(skuCode);
        product.setPrice(price);
        product.setCategoryIds(categoryIds);

        // Then
        assertEquals(id, product.getId());
        assertEquals(name, product.getName());
        assertEquals(description, product.getDescription());
        assertEquals(skuCode, product.getSkuCode());
        assertEquals(price, product.getPrice());
        assertEquals(categoryIds, product.getCategoryIds());
    }

    @Test
    void testProductEqualsAndHashCode() {
        // Given
        List<String> categoryIds = Arrays.asList("cats-001", "cat-food-001");
        
        Product product1 = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(categoryIds)
                .build();

        Product product2 = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(categoryIds)
                .build();

        Product product3 = Product.builder()
                .id("prod-002")
                .name("Dog Treats")
                .description("Healthy treats for dogs")
                .skuCode("DT-001")
                .price(new BigDecimal("12.99"))
                .categoryIds(Arrays.asList("dogs-001", "dog-treats-001"))
                .build();

        // Then
        assertEquals(product1, product2);
        assertEquals(product1.hashCode(), product2.hashCode());
        assertNotEquals(product1, product3);
        assertNotEquals(product1.hashCode(), product3.hashCode());
    }

    @Test
    void testProductToString() {
        // Given
        Product product = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(Arrays.asList("cats-001", "cat-food-001"))
                .build();

        // When
        String toString = product.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("prod-001"));
        assertTrue(toString.contains("Premium Cat Food"));
        assertTrue(toString.contains("CF-001"));
        assertTrue(toString.contains("24.99"));
    }

    @Test
    void testProductWithNullCategoryIds() {
        // Given & When
        Product product = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(null)
                .build();

        // Then
        assertNull(product.getCategoryIds());
    }

    @Test
    void testProductWithEmptyCategoryIds() {
        // Given & When
        Product product = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(Arrays.asList())
                .build();

        // Then
        assertNotNull(product.getCategoryIds());
        assertTrue(product.getCategoryIds().isEmpty());
    }

    @Test
    void testProductWithSingleCategoryId() {
        // Given
        List<String> categoryIds = Arrays.asList("cats-001");

        // When
        Product product = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(categoryIds)
                .build();

        // Then
        assertEquals(1, product.getCategoryIds().size());
        assertEquals("cats-001", product.getCategoryIds().get(0));
    }

    @Test
    void testProductWithMultipleCategoryIds() {
        // Given
        List<String> categoryIds = Arrays.asList("cats-001", "cat-food-001", "premium-food-001");

        // When
        Product product = Product.builder()
                .id("prod-001")
                .name("Premium Cat Food")
                .description("High-quality dry food for adult cats")
                .skuCode("CF-001")
                .price(new BigDecimal("24.99"))
                .categoryIds(categoryIds)
                .build();

        // Then
        assertEquals(3, product.getCategoryIds().size());
        assertTrue(product.getCategoryIds().contains("cats-001"));
        assertTrue(product.getCategoryIds().contains("cat-food-001"));
        assertTrue(product.getCategoryIds().contains("premium-food-001"));
    }

    @Test
    void testProductCategoryAssociation() {
        // Given
        Product product = new Product();
        List<String> initialCategories = Arrays.asList("cats-001");
        List<String> updatedCategories = Arrays.asList("cats-001", "cat-food-001", "premium-food-001");

        // When - Initial assignment
        product.setCategoryIds(initialCategories);

        // Then
        assertEquals(1, product.getCategoryIds().size());
        assertEquals("cats-001", product.getCategoryIds().get(0));

        // When - Update categories
        product.setCategoryIds(updatedCategories);

        // Then
        assertEquals(3, product.getCategoryIds().size());
        assertTrue(product.getCategoryIds().contains("cats-001"));
        assertTrue(product.getCategoryIds().contains("cat-food-001"));
        assertTrue(product.getCategoryIds().contains("premium-food-001"));
    }
}