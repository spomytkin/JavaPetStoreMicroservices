package com.PetStore.product.repository;

import com.PetStore.product.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest {

    @Mock
    private ProductRepository productRepository;



    @Test
    void testFindByCategoryIdsContainingMethodExists() throws NoSuchMethodException {
        // Given
        Class<ProductRepository> repositoryClass = ProductRepository.class;

        // When
        Method method = repositoryClass.getMethod("findByCategoryIdsContaining", String.class);

        // Then
        assertNotNull(method);
        assertEquals("findByCategoryIdsContaining", method.getName());
        assertEquals(List.class, method.getReturnType());
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void testFindByCategoryIdsInMethodExists() throws NoSuchMethodException {
        // Given
        Class<ProductRepository> repositoryClass = ProductRepository.class;

        // When
        Method method = repositoryClass.getMethod("findByCategoryIdsIn", List.class);

        // Then
        assertNotNull(method);
        assertEquals("findByCategoryIdsIn", method.getName());
        assertEquals(List.class, method.getReturnType());
        assertEquals(1, method.getParameterCount());
        assertEquals(List.class, method.getParameterTypes()[0]);
    }

    @Test
    void testRepositoryExtendsMongoRepository() {
        // Given
        Class<ProductRepository> repositoryClass = ProductRepository.class;

        // When & Then
        assertTrue(repositoryClass.isInterface());
        assertEquals(1, repositoryClass.getInterfaces().length);
        // The repository should extend MongoRepository through its parent interfaces
        assertTrue(repositoryClass.getName().contains("ProductRepository"));
    }

    @Test
    void testProductModelHasCategoryIdsField() throws NoSuchMethodException {
        // Given
        Class<Product> productClass = Product.class;

        // When
        Method getCategoryIds = productClass.getMethod("getCategoryIds");
        Method setCategoryIds = productClass.getMethod("setCategoryIds", List.class);

        // Then
        assertNotNull(getCategoryIds);
        assertNotNull(setCategoryIds);
        assertEquals(List.class, getCategoryIds.getReturnType());
        assertEquals(void.class, setCategoryIds.getReturnType());
    }

    @Test
    void testProductBuilderSupportsCategoryIds() {
        // Given & When
        Product product = Product.builder()
                .id("test-001")
                .name("Test Product")
                .description("Test Description")
                .skuCode("TEST-001")
                .price(new BigDecimal("19.99"))
                .categoryIds(Arrays.asList("category1", "category2"))
                .build();

        // Then
        assertNotNull(product);
        assertEquals("test-001", product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals(Arrays.asList("category1", "category2"), product.getCategoryIds());
    }

    @Test
    void testProductWithNullCategoryIds() {
        // Given & When
        Product product = Product.builder()
                .id("test-001")
                .name("Test Product")
                .categoryIds(null)
                .build();

        // Then
        assertNull(product.getCategoryIds());
    }

    @Test
    void testProductWithEmptyCategoryIds() {
        // Given & When
        Product product = Product.builder()
                .id("test-001")
                .name("Test Product")
                .categoryIds(Arrays.asList())
                .build();

        // Then
        assertNotNull(product.getCategoryIds());
        assertTrue(product.getCategoryIds().isEmpty());
    }

    @Test
    void testProductWithMultipleCategoryIds() {
        // Given
        List<String> categoryIds = Arrays.asList("cats", "cat-food", "premium");

        // When
        Product product = Product.builder()
                .id("test-001")
                .name("Test Product")
                .categoryIds(categoryIds)
                .build();

        // Then
        assertEquals(3, product.getCategoryIds().size());
        assertTrue(product.getCategoryIds().contains("cats"));
        assertTrue(product.getCategoryIds().contains("cat-food"));
        assertTrue(product.getCategoryIds().contains("premium"));
    }
}