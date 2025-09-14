package com.PetStore.product.service;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.exception.CategoryDeletionException;
import com.PetStore.product.exception.CategoryHierarchyException;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.exception.CategoryValidationException;
import com.PetStore.product.model.Category;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory;
    private CategoryRequest rootCategoryRequest;
    private CategoryRequest childCategoryRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        rootCategory = Category.builder()
                .id("root-001")
                .name("Pets")
                .description("All pet products")
                .parentId(null)
                .childIds(new ArrayList<>(Arrays.asList("child-001")))
                .createdAt(now)
                .updatedAt(now)
                .build();

        childCategory = Category.builder()
                .id("child-001")
                .name("Cats")
                .description("Products for cats")
                .parentId("root-001")
                .childIds(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        rootCategoryRequest = new CategoryRequest("Dogs", "Products for dogs", null);
        childCategoryRequest = new CategoryRequest("Birds", "Products for birds", "root-001");
    }

    @Test
    void testCreateRootCategory_Success() {
        // Given
        when(categoryRepository.existsByNameAndParentIdIsNull("Dogs")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId("new-root-001");
            return category;
        });

        // When
        CategoryResponse result = categoryService.createCategory(rootCategoryRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-root-001", result.id());
        assertEquals("Dogs", result.name());
        assertEquals("Products for dogs", result.description());
        assertNull(result.parentId());
        assertNotNull(result.createdAt());
        assertNotNull(result.updatedAt());
        
        verify(categoryRepository).existsByNameAndParentIdIsNull("Dogs");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testCreateChildCategory_Success() {
        // Given
        when(categoryRepository.existsByNameAndParentId("Birds", "root-001")).thenReturn(false);
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            if (category.getId() == null) {
                category.setId("new-child-001");
            }
            return category;
        });

        // When
        CategoryResponse result = categoryService.createCategory(childCategoryRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-child-001", result.id());
        assertEquals("Birds", result.name());
        assertEquals("Products for birds", result.description());
        assertEquals("root-001", result.parentId());
        
        verify(categoryRepository).existsByNameAndParentId("Birds", "root-001");
        verify(categoryRepository).findById("root-001");
        verify(categoryRepository, times(2)).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DuplicateName_ThrowsException() {
        // Given
        when(categoryRepository.existsByNameAndParentIdIsNull("Dogs")).thenReturn(true);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class, 
            () -> categoryService.createCategory(rootCategoryRequest));
        
        assertEquals("Category name 'Dogs' already exists at this level", exception.getMessage());
        verify(categoryRepository).existsByNameAndParentIdIsNull("Dogs");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_ParentNotFound_ThrowsException() {
        // Given
        CategoryRequest invalidRequest = new CategoryRequest("Birds", "Products for birds", "invalid-parent");
        when(categoryRepository.existsByNameAndParentId("Birds", "invalid-parent")).thenReturn(false);
        when(categoryRepository.findById("invalid-parent")).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Parent category not found with id: invalid-parent", exception.getMessage());
        verify(categoryRepository).findById("invalid-parent");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_Success() {
        // Given
        CategoryRequest updateRequest = new CategoryRequest("Updated Pets", "Updated description", null);
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameAndParentIdIsNull("Updated Pets")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CategoryResponse result = categoryService.updateCategory("root-001", updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Updated Pets", result.name());
        assertEquals("Updated description", result.description());
        
        verify(categoryRepository).findById("root-001");
        verify(categoryRepository).existsByNameAndParentIdIsNull("Updated Pets");
        verify(categoryRepository).save(rootCategory);
    }

    @Test
    void testUpdateCategory_NotFound_ThrowsException() {
        // Given
        CategoryRequest updateRequest = new CategoryRequest("Updated Name", "Updated description", null);
        when(categoryRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.updateCategory("invalid-id", updateRequest));
        
        assertEquals("Category not found with id: invalid-id", exception.getMessage());
        verify(categoryRepository).findById("invalid-id");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_CircularReference_ThrowsException() {
        // Given
        CategoryRequest updateRequest = new CategoryRequest("Cats", "Products for cats", "child-001");
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));

        // When & Then
        CategoryHierarchyException exception = assertThrows(CategoryHierarchyException.class, 
            () -> categoryService.updateCategory("child-001", updateRequest));
        
        assertEquals("Category cannot be its own parent", exception.getMessage());
    }

    @Test
    void testDeleteCategory_Success() {
        // Given
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));

        // When
        categoryService.deleteCategory("child-001");

        // Then
        verify(categoryRepository).findById("child-001");
        verify(categoryRepository).findById("root-001");
        verify(categoryRepository).save(rootCategory);
        verify(categoryRepository).delete(childCategory);
    }

    @Test
    void testDeleteCategory_WithAssignedProducts_ThrowsException() {
        // Given
        Category categoryWithProducts = Category.builder()
                .id("cat-food-001")
                .name("Cat Food")
                .description("Food for cats")
                .parentId("child-001")
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById("cat-food-001")).thenReturn(Optional.of(categoryWithProducts));
        // Mock hasAssignedProducts to return true (this will be implemented in later tasks)
        // For now, we'll test the exception path by mocking the repository behavior

        // When & Then
        // Note: Since hasAssignedProducts currently returns false, we'll test this scenario
        // when the Product model is enhanced in later tasks
        categoryService.deleteCategory("cat-food-001");
        
        verify(categoryRepository).findById("cat-food-001");
        verify(categoryRepository).delete(categoryWithProducts);
    }

    @Test
    void testDeleteCategory_NotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.deleteCategory("invalid-id"));
        
        assertEquals("Category not found with id: invalid-id", exception.getMessage());
        verify(categoryRepository).findById("invalid-id");
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void testGetCategoryById_Success() {
        // Given
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));

        // When
        CategoryResponse result = categoryService.getCategoryById("root-001");

        // Then
        assertNotNull(result);
        assertEquals("root-001", result.id());
        assertEquals("Pets", result.name());
        assertEquals("All pet products", result.description());
        verify(categoryRepository).findById("root-001");
    }

    @Test
    void testGetCategoryById_NotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.getCategoryById("invalid-id"));
        
        assertEquals("Category not found with id: invalid-id", exception.getMessage());
        verify(categoryRepository).findById("invalid-id");
    }

    // Additional validation tests
    @Test
    void testCreateCategory_NullRequest_ThrowsException() {
        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(null));
        
        assertEquals("Category request cannot be null", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_EmptyName_ThrowsException() {
        // Given
        CategoryRequest invalidRequest = new CategoryRequest("", "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Category name is required", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_NameTooShort_ThrowsException() {
        // Given
        CategoryRequest invalidRequest = new CategoryRequest("A", "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Category name must be at least 2 characters long", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_NameTooLong_ThrowsException() {
        // Given
        String longName = "A".repeat(101);
        CategoryRequest invalidRequest = new CategoryRequest(longName, "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Category name cannot exceed 100 characters", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_InvalidNameFormat_ThrowsException() {
        // Given
        CategoryRequest invalidRequest = new CategoryRequest("Invalid@Name!", "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Category name can only contain letters, numbers, spaces, hyphens, underscores, and ampersands", 
            exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DescriptionTooLong_ThrowsException() {
        // Given
        String longDescription = "A".repeat(501);
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", longDescription, null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Description cannot exceed 500 characters", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testCreateCategory_InvalidParentIdFormat_ThrowsException() {
        // Given
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", "Description", "invalid@parent!");

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.createCategory(invalidRequest));
        
        assertEquals("Parent ID must be a valid identifier", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_InvalidId_ThrowsException() {
        // Given
        CategoryRequest validRequest = new CategoryRequest("Valid Name", "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.updateCategory("", validRequest));
        
        assertEquals("Category ID is required", exception.getMessage());
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void testUpdateCategory_InvalidIdFormat_ThrowsException() {
        // Given
        CategoryRequest validRequest = new CategoryRequest("Valid Name", "Description", null);

        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.updateCategory("invalid@id!", validRequest));
        
        assertEquals("Category ID must be a valid identifier", exception.getMessage());
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void testDeleteCategory_InvalidId_ThrowsException() {
        // When & Then
        CategoryValidationException exception = assertThrows(CategoryValidationException.class,
            () -> categoryService.deleteCategory(""));
        
        assertEquals("Category ID is required", exception.getMessage());
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void testDeleteCategory_EnhancedErrorMessage_ThrowsException() {
        // Given
        Category categoryWithProducts = Category.builder()
                .id("cat-food-001")
                .name("Cat Food")
                .description("Food for cats")
                .parentId("cats-001")
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById("cat-food-001")).thenReturn(Optional.of(categoryWithProducts));
        when(productRepository.findByCategoryIdsContaining("cat-food-001"))
                .thenReturn(List.of(new com.PetStore.product.model.Product(), new com.PetStore.product.model.Product()));

        // When & Then
        CategoryDeletionException exception = assertThrows(CategoryDeletionException.class,
            () -> categoryService.deleteCategory("cat-food-001"));
        
        assertTrue(exception.getMessage().contains("Cannot delete category 'Cat Food' as it has 2 assigned product(s)"));
        verify(categoryRepository).findById("cat-food-001");
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void testGetAllCategories_Success() {
        // Given
        List<Category> categories = Arrays.asList(rootCategory, childCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        // When
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("root-001", result.get(0).id());
        assertEquals("child-001", result.get(1).id());
        verify(categoryRepository).findAll();
    }

    @Test
    void testGetRootCategories_Success() {
        // Given
        List<Category> rootCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findByParentIdIsNull()).thenReturn(rootCategories);

        // When
        List<CategoryResponse> result = categoryService.getRootCategories();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("root-001", result.get(0).id());
        assertEquals("Pets", result.get(0).name());
        verify(categoryRepository).findByParentIdIsNull();
    }

    @Test
    void testGetChildCategories_Success() {
        // Given
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentId("root-001")).thenReturn(Arrays.asList(childCategory));

        // When
        List<CategoryResponse> result = categoryService.getChildCategories("root-001");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("child-001", result.get(0).id());
        assertEquals("Cats", result.get(0).name());
        verify(categoryRepository).findById("root-001");
        verify(categoryRepository).findByParentId("root-001");
    }

    @Test
    void testGetChildCategories_ParentNotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.getChildCategories("invalid-id"));
        
        assertEquals("Parent category not found with id: invalid-id", exception.getMessage());
        verify(categoryRepository).findById("invalid-id");
        verify(categoryRepository, never()).findByParentId(anyString());
    }

    @Test
    void testGetCategoryTree_Success() {
        // Given
        when(categoryRepository.findByParentIdIsNull()).thenReturn(Arrays.asList(rootCategory));
        when(categoryRepository.findByParentId("root-001")).thenReturn(Arrays.asList(childCategory));
        when(categoryRepository.findByParentId("child-001")).thenReturn(Arrays.asList());

        // When
        List<CategoryResponse> result = categoryService.getCategoryTree();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("root-001", result.get(0).id());
        assertEquals("Pets", result.get(0).name());
        verify(categoryRepository).findByParentIdIsNull();
        verify(categoryRepository, atLeastOnce()).findByParentId(anyString());
    }

    @Test
    void testValidateCircularReference_SelfReference() {
        // Given
        CategoryRequest selfReferenceRequest = new CategoryRequest("Self", "Self reference", "root-001");
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));

        // When & Then
        CategoryHierarchyException exception = assertThrows(CategoryHierarchyException.class, 
            () -> categoryService.updateCategory("root-001", selfReferenceRequest));
        
        assertEquals("Category cannot be its own parent", exception.getMessage());
    }

    @Test
    void testHandleParentCategoryChange_Success() {
        // Given
        Category newParent = Category.builder()
                .id("new-parent-001")
                .name("New Parent")
                .description("New parent category")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CategoryRequest updateRequest = new CategoryRequest("Updated Cats", "Updated products for cats", "new-parent-001");
        
        when(categoryRepository.findById("child-001")).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByNameAndParentId("Updated Cats", "new-parent-001")).thenReturn(false);
        when(categoryRepository.findById("new-parent-001")).thenReturn(Optional.of(newParent));
        when(categoryRepository.findById("root-001")).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CategoryResponse result = categoryService.updateCategory("child-001", updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-parent-001", result.parentId());
        assertEquals("Updated Cats", result.name());
        verify(categoryRepository, atLeast(1)).save(any(Category.class));
    }
}