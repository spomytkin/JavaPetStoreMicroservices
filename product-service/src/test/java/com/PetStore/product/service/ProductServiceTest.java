package com.PetStore.product.service;

import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.model.Category;
import com.PetStore.product.model.Product;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id("category1")
                .name("Dogs")
                .description("Products for dogs")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProduct = Product.builder()
                .id("product1")
                .name("Dog Food")
                .description("Premium dog food")
                .skuCode("DF-001")
                .price(new BigDecimal("29.99"))
                .categoryIds(Arrays.asList("category1"))
                .build();

        testProductRequest = new ProductRequest(
                null,
                "Dog Food",
                "Premium dog food",
                "DF-001",
                new BigDecimal("29.99"),
                Arrays.asList("category1")
        );
    }

    @Test
    @DisplayName("Should create product successfully with valid categories")
    void shouldCreateProductWithValidCategories() {
        // Given
        when(categoryRepository.existsById("category1")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductResponse result = productService.createProduct(testProductRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("product1");
        assertThat(result.name()).isEqualTo("Dog Food");
        assertThat(result.description()).isEqualTo("Premium dog food");
        assertThat(result.skuCode()).isEqualTo("DF-001");
        assertThat(result.price()).isEqualTo(new BigDecimal("29.99"));
        assertThat(result.categoryIds()).containsExactly("category1");

        verify(categoryRepository).existsById("category1");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should create product successfully with no categories")
    void shouldCreateProductWithNoCategories() {
        // Given
        ProductRequest requestWithoutCategories = new ProductRequest(
                null,
                "Dog Food",
                "Premium dog food",
                "DF-001",
                new BigDecimal("29.99"),
                null
        );

        Product productWithoutCategories = Product.builder()
                .id("product1")
                .name("Dog Food")
                .description("Premium dog food")
                .skuCode("DF-001")
                .price(new BigDecimal("29.99"))
                .categoryIds(new ArrayList<>())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(productWithoutCategories);

        // When
        ProductResponse result = productService.createProduct(requestWithoutCategories);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.categoryIds()).isEmpty();

        verify(categoryRepository, never()).existsById(anyString());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when creating product with invalid category")
    void shouldThrowExceptionWhenCreatingProductWithInvalidCategory() {
        // Given
        when(categoryRepository.existsById("category1")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductRequest))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found with id: category1");

        verify(categoryRepository).existsById("category1");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products successfully")
    void shouldGetAllProductsSuccessfully() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<ProductResponse> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("product1");
        assertThat(result.get(0).name()).isEqualTo("Dog Food");
        assertThat(result.get(0).categoryIds()).containsExactly("category1");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should get products by category successfully")
    void shouldGetProductsByCategorySuccessfully() {
        // Given
        String categoryId = "category1";
        List<Product> products = Arrays.asList(testProduct);
        
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.findByCategoryIdsContaining(categoryId)).thenReturn(products);

        // When
        List<ProductResponse> result = productService.getProductsByCategory(categoryId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("product1");
        assertThat(result.get(0).categoryIds()).containsExactly("category1");

        verify(categoryRepository).existsById(categoryId);
        verify(productRepository).findByCategoryIdsContaining(categoryId);
    }

    @Test
    @DisplayName("Should throw exception when getting products by invalid category")
    void shouldThrowExceptionWhenGettingProductsByInvalidCategory() {
        // Given
        String categoryId = "invalid-category";
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productService.getProductsByCategory(categoryId))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found with id: invalid-category");

        verify(categoryRepository).existsById(categoryId);
        verify(productRepository, never()).findByCategoryIdsContaining(anyString());
    }

    @Test
    @DisplayName("Should get products by category hierarchy successfully")
    void shouldGetProductsByCategoryHierarchySuccessfully() {
        // Given
        String parentCategoryId = "category1";
        String childCategoryId = "category2";
        
        Category childCategory = Category.builder()
                .id(childCategoryId)
                .name("Dog Food")
                .description("Food for dogs")
                .parentId(parentCategoryId)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product childProduct = Product.builder()
                .id("product2")
                .name("Puppy Food")
                .description("Food for puppies")
                .skuCode("PF-001")
                .price(new BigDecimal("19.99"))
                .categoryIds(Arrays.asList(childCategoryId))
                .build();

        List<Product> allProducts = Arrays.asList(testProduct, childProduct);
        
        when(categoryRepository.existsById(parentCategoryId)).thenReturn(true);
        when(categoryRepository.findByParentId(parentCategoryId)).thenReturn(Arrays.asList(childCategory));
        when(categoryRepository.findByParentId(childCategoryId)).thenReturn(new ArrayList<>());
        when(productRepository.findByCategoryIdsIn(any())).thenReturn(allProducts);

        // When
        List<ProductResponse> result = productService.getProductsByCategoryHierarchy(parentCategoryId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::id).containsExactlyInAnyOrder("product1", "product2");

        verify(categoryRepository).existsById(parentCategoryId);
        verify(categoryRepository).findByParentId(parentCategoryId);
        verify(productRepository).findByCategoryIdsIn(any());
    }

    @Test
    @DisplayName("Should throw exception when getting products by invalid category hierarchy")
    void shouldThrowExceptionWhenGettingProductsByInvalidCategoryHierarchy() {
        // Given
        String categoryId = "invalid-category";
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productService.getProductsByCategoryHierarchy(categoryId))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found with id: invalid-category");

        verify(categoryRepository).existsById(categoryId);
        verify(categoryRepository, never()).findByParentId(anyString());
        verify(productRepository, never()).findByCategoryIdsIn(any());
    }

    @Test
    @DisplayName("Should handle empty category hierarchy")
    void shouldHandleEmptyCategoryHierarchy() {
        // Given
        String categoryId = "category1";
        
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryRepository.findByParentId(categoryId)).thenReturn(new ArrayList<>());
        when(productRepository.findByCategoryIdsIn(Arrays.asList(categoryId))).thenReturn(Arrays.asList(testProduct));

        // When
        List<ProductResponse> result = productService.getProductsByCategoryHierarchy(categoryId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("product1");

        verify(categoryRepository).existsById(categoryId);
        verify(categoryRepository).findByParentId(categoryId);
        verify(productRepository).findByCategoryIdsIn(Arrays.asList(categoryId));
    }

    @Test
    @DisplayName("Should create product with multiple categories")
    void shouldCreateProductWithMultipleCategories() {
        // Given
        ProductRequest multiCategoryRequest = new ProductRequest(
                null,
                "Multi-Category Product",
                "Product in multiple categories",
                "MC-001",
                new BigDecimal("39.99"),
                Arrays.asList("category1", "category2")
        );

        Product multiCategoryProduct = Product.builder()
                .id("product1")
                .name("Multi-Category Product")
                .description("Product in multiple categories")
                .skuCode("MC-001")
                .price(new BigDecimal("39.99"))
                .categoryIds(Arrays.asList("category1", "category2"))
                .build();

        when(categoryRepository.existsById("category1")).thenReturn(true);
        when(categoryRepository.existsById("category2")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(multiCategoryProduct);

        // When
        ProductResponse result = productService.createProduct(multiCategoryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.categoryIds()).containsExactlyInAnyOrder("category1", "category2");

        verify(categoryRepository).existsById("category1");
        verify(categoryRepository).existsById("category2");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should return empty list when no products found in category")
    void shouldReturnEmptyListWhenNoProductsFoundInCategory() {
        // Given
        String categoryId = "category1";
        
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.findByCategoryIdsContaining(categoryId)).thenReturn(new ArrayList<>());

        // When
        List<ProductResponse> result = productService.getProductsByCategory(categoryId);

        // Then
        assertThat(result).isEmpty();

        verify(categoryRepository).existsById(categoryId);
        verify(productRepository).findByCategoryIdsContaining(categoryId);
    }
}