package com.PetStore.product.integration;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.model.Category;
import com.PetStore.product.model.Product;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import com.PetStore.product.service.CategoryService;
import com.PetStore.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

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
 * Comprehensive integration tests for the complete category-product workflow.
 * Tests end-to-end functionality including category creation, hierarchy management,
 * product assignment, filtering, and data integrity constraints using mocked repositories.
 */
@SpringBootTest
@ActiveProfiles("test")
class CategoryProductWorkflowMockIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private ProductRepository productRepository;

    private Category rootCategory;
    private Category childCategory;
    private Category grandchildCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        reset(categoryRepository, productRepository);
        setupTestData();
    }

    private void setupTestData() {
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
                .childIds(new ArrayList<>(List.of("grandchild-001")))
                .createdAt(now)
                .updatedAt(now)
                .build();

        grandchildCategory = Category.builder()
                .id("grandchild-001")
                .name("Dog Food")
                .description("Food for dogs")
                .parentId("child-001")
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
                .categoryIds(List.of("grandchild-001", "root-001"))
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

        // Step 3: Create grandchild category
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByNameAndParentId("Dog Food", "child-001")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return Category.builder()
                    .id("grandchild-001")
                    .name(category.getName())
                    .description(category.getDescription())
                    .parentId(category.getParentId())
                    .childIds(category.getChildIds())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        });

        CategoryResponse createdGrandchild = categoryService.createCategory(
                new CategoryRequest("Dog Food", "Food for dogs", "child-001")
        );

        assertThat(createdGrandchild.name()).isEqualTo("Dog Food");
        assertThat(createdGrandchild.parentId()).isEqualTo("child-001");

        // Step 4: Create product assigned to categories
        when(categoryRepository.findAllById(List.of("grandchild-001", "root-001")))
                .thenReturn(List.of(grandchildCategory, rootCategory));
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
                List.of("grandchild-001", "root-001")
        ));

        assertThat(createdProduct.name()).isEqualTo("Premium Dog Food");
        assertThat(createdProduct.categoryIds()).containsExactlyInAnyOrder("grandchild-001", "root-001");

        // Step 5: Test product filtering by category
        when(productRepository.findByCategoryIdsContaining("root-001"))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> rootProducts = productService.getProductsByCategory("root-001");
        assertThat(rootProducts).hasSize(1);
        assertThat(rootProducts.get(0).id()).isEqualTo("product-001");

        when(productRepository.findByCategoryIdsContaining("grandchild-001"))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> grandchildProducts = productService.getProductsByCategory("grandchild-001");
        assertThat(grandchildProducts).hasSize(1);
        assertThat(grandchildProducts.get(0).id()).isEqualTo("product-001");

        // Step 6: Test category hierarchy filtering
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));
        when(productRepository.findByCategoryIdsIn(List.of("child-001", "grandchild-001")))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> hierarchyProducts = productService.getProductsByCategoryHierarchy("child-001");
        assertThat(hierarchyProducts).hasSize(1);
        assertThat(hierarchyProducts.get(0).id()).isEqualTo("product-001");

        // Verify all expected repository interactions
        verify(categoryRepository, times(3)).save(any(Category.class));
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productRepository, times(2)).findByCategoryIdsContaining(anyString());
        verify(productRepository, times(1)).findByCategoryIdsIn(anyList());
    }

    @Test
    void categoryDeletion_WithAssignedProducts_ShouldPreventDeletion() {
        // Setup: Category with assigned products
        when(categoryRepository.findById("grandchild-001")).thenReturn(Optional.of(grandchildCategory));
        when(productRepository.findByCategoryIdsContaining("grandchild-001"))
                .thenReturn(List.of(testProduct));

        // Attempt to delete category with assigned products
        assertThatThrownBy(() -> categoryService.deleteCategory("grandchild-001"))
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
        when(categoryRepository.findById("grandchild-001")).thenReturn(Optional.of(grandchildCategory));

        // Attempt to create circular reference
        assertThatThrownBy(() -> categoryService.updateCategory("root-001", 
                new CategoryRequest("Pets", "Updated description", "grandchild-001")))
                .hasMessageContaining("circular reference");

        // Verify no update was performed
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void productCategoryAssignment_MultipleOperations_ShouldMaintainConsistency() {
        // Create additional categories for testing
        Category category2 = Category.builder()
                .id("cat-001")
                .name("Cats")
                .description("Products for cats")
                .parentId("root-001")
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category category3 = Category.builder()
                .id("accessories-001")
                .name("Accessories")
                .description("Pet accessories")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Step 1: Create product with initial categories
        when(categoryRepository.findAllById(List.of("grandchild-001", "root-001")))
                .thenReturn(List.of(grandchildCategory, rootCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return Product.builder()
                    .id("multi-product-001")
                    .name(product.getName())
                    .description(product.getDescription())
                    .skuCode(product.getSkuCode())
                    .price(product.getPrice())
                    .categoryIds(product.getCategoryIds())
                    .build();
        });

        ProductResponse product = productService.createProduct(new ProductRequest(
                null,
                "Multi-Category Product",
                "Product in multiple categories",
                "MULTI-001",
                new BigDecimal("29.99"),
                List.of("grandchild-001", "root-001")
        ));

        // Step 2: Verify filtering by different categories
        when(productRepository.findByCategoryIdsContaining("grandchild-001")).thenReturn(List.of(testProduct));
        when(productRepository.findByCategoryIdsContaining("cat-001")).thenReturn(List.of());

        List<ProductResponse> grandchildProducts = productService.getProductsByCategory("grandchild-001");
        assertThat(grandchildProducts).hasSize(1);
        assertThat(grandchildProducts.get(0).id()).isEqualTo("multi-product-001");

        List<ProductResponse> catProducts = productService.getProductsByCategory("cat-001");
        assertThat(catProducts).isEmpty();
    }

    @Test
    void categoryTreeRetrieval_ComplexHierarchy_ShouldBuildCorrectStructure() {
        // Setup complex hierarchy
        List<Category> allCategories = List.of(rootCategory, childCategory, grandchildCategory);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        List<CategoryResponse> categoryTree = categoryService.getCategoryTree();

        // Verify tree structure
        assertThat(categoryTree).hasSize(1); // One root category
        CategoryResponse rootInTree = categoryTree.get(0);
        assertThat(rootInTree.name()).isEqualTo("Pets");
        assertThat(rootInTree.childIds()).contains("child-001");
    }

    @Test
    void childCategoryRetrieval_ShouldReturnDirectChildren() {
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentId("root-001")).thenReturn(List.of(childCategory));

        List<CategoryResponse> children = categoryService.getChildCategories("root-001");

        assertThat(children).hasSize(1);
        assertThat(children.get(0).name()).isEqualTo("Dogs");
        assertThat(children.get(0).parentId()).isEqualTo("root-001");
    }

    @Test
    void dataIntegrityValidation_InvalidCategoryAssignment_ShouldThrowException() {
        // Attempt to create product with non-existent category
        when(categoryRepository.findAllById(List.of("non-existent-001")))
                .thenReturn(List.of()); // Empty list indicates category doesn't exist

        assertThatThrownBy(() -> productService.createProduct(new ProductRequest(
                null,
                "Invalid Product",
                "Product with invalid category",
                "INVALID-001",
                new BigDecimal("19.99"),
                List.of("non-existent-001")
        ))).hasMessageContaining("not found");

        // Verify no product was saved
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void largeHierarchyOperations_PerformanceAndConsistency_ShouldHandleComplexStructures() {
        // Create a complex hierarchy structure
        List<Category> complexHierarchy = new ArrayList<>();
        
        // Root category
        Category root = Category.builder()
                .id("complex-root")
                .name("Complex Root")
                .description("Root of complex hierarchy")
                .parentId(null)
                .childIds(new ArrayList<>(List.of("level1-1", "level1-2", "level1-3")))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        complexHierarchy.add(root);

        // Level 1 categories (3 categories)
        for (int i = 1; i <= 3; i++) {
            Category level1 = Category.builder()
                    .id("level1-" + i)
                    .name("Level1 Category " + i)
                    .description("Level 1 category")
                    .parentId("complex-root")
                    .childIds(new ArrayList<>(List.of("level2-" + i + "-1", "level2-" + i + "-2")))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            complexHierarchy.add(level1);

            // Level 2 categories (2 per level 1 = 6 total)
            for (int j = 1; j <= 2; j++) {
                Category level2 = Category.builder()
                        .id("level2-" + i + "-" + j)
                        .name("Level2 Category " + i + "-" + j)
                        .description("Level 2 category")
                        .parentId("level1-" + i)
                        .childIds(new ArrayList<>())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                complexHierarchy.add(level2);
            }
        }

        when(categoryRepository.findAll()).thenReturn(complexHierarchy);

        // Test tree retrieval for complex structure
        List<CategoryResponse> tree = categoryService.getCategoryTree();
        assertThat(tree).hasSize(1); // One root
        assertThat(tree.get(0).childIds()).hasSize(3); // Three level 1 children

        // Test hierarchy-based product filtering
        when(categoryRepository.findById("level1-1")).thenReturn(Optional.of(complexHierarchy.get(1)));
        when(productRepository.findByCategoryIdsIn(List.of("level1-1", "level2-1-1", "level2-1-2")))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> hierarchyProducts = productService.getProductsByCategoryHierarchy("level1-1");
        assertThat(hierarchyProducts).hasSize(1);

        // Verify performance - should handle complex queries efficiently
        verify(categoryRepository, times(1)).findById("level1-1");
        verify(productRepository, times(1)).findByCategoryIdsIn(anyList());
    }
}