package com.PetStore.product.integration;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.exception.CategoryDeletionException;
import com.PetStore.product.exception.CategoryHierarchyException;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.model.Category;
import com.PetStore.product.model.Product;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import com.PetStore.product.service.CategoryService;
import com.PetStore.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the complete category-product workflow.
 * These tests verify the integration between CategoryService and ProductService
 * without requiring a full Spring Boot application context.
 */
@ExtendWith(MockitoExtension.class)
class CategoryProductWorkflowUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private Category rootCategory;
    private Category childCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        rootCategory = Category.builder()
                .id("root-001")
                .name("Pets")
                .description("All pet products")
                .parentId(null)
                .childIds(new ArrayList<>(List.of("child-001")))
                .createdAt(now)
                .updatedAt(now)
                .build();

        childCategory = Category.builder()
                .id("child-001")
                .name("Dogs")
                .description("Products for dogs")
                .parentId("root-001")
                .childIds(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        testProduct = Product.builder()
                .id("product-001")
                .name("Premium Dog Food")
                .description("High-quality dry food")
                .skuCode("DF-001")
                .price(new BigDecimal("45.99"))
                .categoryIds(List.of("child-001", "root-001"))
                .build();
    }

    @Test
    void completeWorkflow_CreateCategoryHierarchyAndAssignProducts_ShouldWorkEndToEnd() {
        // Step 1: Create root category
        when(categoryRepository.existsByNameAndParentId("Pets", null)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return Category.builder()
                    .id("root-001")
                    .name(category.getName())
                    .description(category.getDescription())
                    .parentId(category.getParentId())
                    .childIds(category.getChildIds())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        });

        CategoryResponse createdRoot = categoryService.createCategory(
                new CategoryRequest("Pets", "All pet products", null)
        );

        assertThat(createdRoot.name()).isEqualTo("Pets");
        assertThat(createdRoot.parentId()).isNull();

        // Step 2: Create child category
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameAndParentId("Dogs", "root-001")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return Category.builder()
                    .id("child-001")
                    .name(category.getName())
                    .description(category.getDescription())
                    .parentId(category.getParentId())
                    .childIds(category.getChildIds())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        });

        CategoryResponse createdChild = categoryService.createCategory(
                new CategoryRequest("Dogs", "Products for dogs", "root-001")
        );

        assertThat(createdChild.name()).isEqualTo("Dogs");
        assertThat(createdChild.parentId()).isEqualTo("root-001");

        // Step 3: Create product assigned to categories
        when(categoryRepository.findAllById(List.of("child-001", "root-001")))
                .thenReturn(List.of(childCategory, rootCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return Product.builder()
                    .id("product-001")
                    .name(product.getName())
                    .description(product.getDescription())
                    .skuCode(product.getSkuCode())
                    .price(product.getPrice())
                    .categoryIds(product.getCategoryIds())
                    .build();
        });

        ProductResponse createdProduct = productService.createProduct(new ProductRequest(
                null,
                "Premium Dog Food",
                "High-quality dry food",
                "DF-001",
                new BigDecimal("45.99"),
                List.of("child-001", "root-001")
        ));

        assertThat(createdProduct.name()).isEqualTo("Premium Dog Food");
        assertThat(createdProduct.categoryIds()).containsExactlyInAnyOrder("child-001", "root-001");

        // Step 4: Test product filtering by category
        when(categoryRepository.existsById("root-001")).thenReturn(true);
        when(productRepository.findByCategoryIdsContaining("root-001"))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> rootProducts = productService.getProductsByCategory("root-001");
        assertThat(rootProducts).hasSize(1);
        assertThat(rootProducts.get(0).id()).isEqualTo("product-001");

        // Verify all expected repository interactions
        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productRepository, times(1)).findByCategoryIdsContaining("root-001");
    }

    @Test
    void categoryDeletion_WithAssignedProducts_ShouldPreventDeletion() {
        // Setup: Category with assigned products
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));
        when(productRepository.findByCategoryIdsContaining("child-001"))
                .thenReturn(List.of(testProduct));

        // Attempt to delete category with assigned products
        assertThatThrownBy(() -> categoryService.deleteCategory("child-001"))
                .isInstanceOf(CategoryDeletionException.class)
                .hasMessageContaining("Cannot delete category")
                .hasMessageContaining("assigned product");

        // Verify category was not deleted
        verify(categoryRepository, never()).delete(any(Category.class));
        verify(categoryRepository, never()).deleteById(anyString());
    }

    @Test
    void categoryDeletion_WithoutAssignedProducts_ShouldSucceedAndUpdateHierarchy() {
        // Setup: Category without assigned products
        Category emptyCategory = Category.builder()
                .id("empty-001")
                .name("Empty Category")
                .description("Category with no products")
                .parentId("root-001")
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById("empty-001")).thenReturn(Optional.of(emptyCategory));
        when(productRepository.findByCategoryIdsContaining("empty-001")).thenReturn(List.of());
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));

        // Delete empty category
        categoryService.deleteCategory("empty-001");

        // Verify category was deleted and parent was updated
        verify(categoryRepository, times(1)).delete(emptyCategory);
        verify(categoryRepository, times(1)).save(any(Category.class)); // Parent update
    }

    @Test
    void circularReferencePreventionInHierarchy_ShouldThrowException() {
        // Setup: Try to make parent a child of its descendant
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));

        // Attempt to create circular reference
        assertThatThrownBy(() -> categoryService.updateCategory("root-001", 
                new CategoryRequest("Pets", "Updated description", "child-001")))
                .isInstanceOf(CategoryHierarchyException.class)
                .hasMessageContaining("circular reference");

        // Verify no update was performed
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void productCategoryAssignment_ValidCategories_ShouldSucceed() {
        // Setup: Valid categories exist
        when(categoryRepository.findAllById(List.of("child-001", "root-001")))
                .thenReturn(List.of(childCategory, rootCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create product with category assignments
        ProductResponse product = productService.createProduct(new ProductRequest(
                null,
                "Test Product",
                "Test description",
                "TEST-001",
                new BigDecimal("29.99"),
                List.of("child-001", "root-001")
        ));

        assertThat(product.categoryIds()).containsExactlyInAnyOrder("child-001", "root-001");
        verify(categoryRepository, times(1)).findAllById(List.of("child-001", "root-001"));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void productCategoryAssignment_InvalidCategory_ShouldThrowException() {
        // Setup: Category doesn't exist
        when(categoryRepository.findAllById(List.of("non-existent-001")))
                .thenReturn(List.of()); // Empty list indicates category doesn't exist

        // Attempt to create product with invalid category
        assertThatThrownBy(() -> productService.createProduct(new ProductRequest(
                null,
                "Invalid Product",
                "Product with invalid category",
                "INVALID-001",
                new BigDecimal("19.99"),
                List.of("non-existent-001")
        ))).isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("not found");

        // Verify no product was saved
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void categoryHierarchyOperations_GetChildCategories_ShouldReturnDirectChildren() {
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentId("root-001")).thenReturn(List.of(childCategory));

        List<CategoryResponse> children = categoryService.getChildCategories("root-001");

        assertThat(children).hasSize(1);
        assertThat(children.get(0).name()).isEqualTo("Dogs");
        assertThat(children.get(0).parentId()).isEqualTo("root-001");
    }

    @Test
    void categoryTreeRetrieval_ShouldBuildCorrectStructure() {
        List<Category> allCategories = List.of(rootCategory, childCategory);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        List<CategoryResponse> categoryTree = categoryService.getCategoryTree();

        // Verify tree structure
        assertThat(categoryTree).hasSize(1); // One root category
        CategoryResponse rootInTree = categoryTree.get(0);
        assertThat(rootInTree.name()).isEqualTo("Pets");
        assertThat(rootInTree.childIds()).contains("child-001");
    }

    @Test
    void productFiltering_ByCategoryHierarchy_ShouldIncludeSubcategories() {
        // Setup: Category hierarchy exists
        when(categoryRepository.existsById("root-001")).thenReturn(true);
        when(categoryRepository.findByParentId("root-001")).thenReturn(List.of(childCategory));
        when(categoryRepository.findByParentId("child-001")).thenReturn(List.of());
        when(productRepository.findByCategoryIdsIn(List.of("root-001", "child-001")))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> hierarchyProducts = productService.getProductsByCategoryHierarchy("root-001");

        assertThat(hierarchyProducts).hasSize(1);
        assertThat(hierarchyProducts.get(0).id()).isEqualTo("product-001");
        verify(productRepository, times(1)).findByCategoryIdsIn(anyList());
    }
}