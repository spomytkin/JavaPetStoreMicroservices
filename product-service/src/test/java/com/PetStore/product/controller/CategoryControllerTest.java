package com.PetStore.product.controller;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.dto.CategoryTreeResponse;
import com.PetStore.product.exception.CategoryDeletionException;
import com.PetStore.product.exception.CategoryHierarchyException;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.exception.CategoryValidationException;
import com.PetStore.product.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryRequest validCategoryRequest;
    private CategoryResponse categoryResponse;
    private CategoryTreeResponse rootCategoryTreeResponse;
    private CategoryResponse childCategoryResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        validCategoryRequest = new CategoryRequest(
                "Test Category",
                "Test Description",
                null
        );

        categoryResponse = new CategoryResponse(
                "category-id-1",
                "Test Category",
                "Test Description",
                null,
                List.of(),
                now,
                now
        );

        rootCategoryTreeResponse = new CategoryTreeResponse(
                "root-id",
                "Root Category",
                "Root Description",
                null,
                List.of(),
                now,
                now
        );

        childCategoryResponse = new CategoryResponse(
                "child-id-1",
                "Child Category",
                "Child Description",
                "root-id",
                List.of(),
                now,
                now
        );
    }

    @Test
    void createCategory_WithValidRequest_ShouldReturnCreated() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenReturn(categoryResponse);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCategoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("category-id-1"))
                .andExpect(jsonPath("$.name").value("Test Category"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.parentId").isEmpty());

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategory_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategory_WithValidationException_ShouldReturnBadRequest() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new CategoryValidationException("Category name already exists"));

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCategoryRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Category name already exists"));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void getAllCategories_ShouldReturnOkWithCategoryList() throws Exception {
        List<CategoryResponse> categories = Arrays.asList(rootCategoryResponse, childCategoryResponse);
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("root-id"))
                .andExpect(jsonPath("$[1].id").value("child-id-1"));

        verify(categoryService).getAllCategories();
    }

    @Test
    void getCategoryById_WithValidId_ShouldReturnOk() throws Exception {
        when(categoryService.getCategoryById("category-id-1"))
                .thenReturn(categoryResponse);

        mockMvc.perform(get("/api/category/category-id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("category-id-1"))
                .andExpect(jsonPath("$.name").value("Test Category"));

        verify(categoryService).getCategoryById("category-id-1");
    }

    @Test
    void getCategoryById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        when(categoryService.getCategoryById("invalid-id"))
                .thenThrow(new CategoryNotFoundException("Category not found with id: invalid-id"));

        mockMvc.perform(get("/api/category/invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: invalid-id"));

        verify(categoryService).getCategoryById("invalid-id");
    }

    @Test
    void getCategoryTree_ShouldReturnOkWithTreeStructure() throws Exception {
        List<CategoryTreeResponse> tree = List.of(rootCategoryTreeResponse);
        when(categoryService.getCategoryTree()).thenReturn(tree);

        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("root-id"))
                .andExpect(jsonPath("$[0].childIds.length()").value(2));

        verify(categoryService).getCategoryTree();
    }

    @Test
    void getChildCategories_WithValidParentId_ShouldReturnOk() throws Exception {
        List<CategoryResponse> children = List.of(childCategoryResponse);
        when(categoryService.getChildCategories("root-id")).thenReturn(children);

        mockMvc.perform(get("/api/category/root-id/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("child-id-1"))
                .andExpect(jsonPath("$[0].parentId").value("root-id"));

        verify(categoryService).getChildCategories("root-id");
    }

    @Test
    void getChildCategories_WithInvalidParentId_ShouldReturnNotFound() throws Exception {
        when(categoryService.getChildCategories("invalid-id"))
                .thenThrow(new CategoryNotFoundException("Parent category not found with id: invalid-id"));

        mockMvc.perform(get("/api/category/invalid-id/children"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Parent category not found with id: invalid-id"));

        verify(categoryService).getChildCategories("invalid-id");
    }

    @Test
    void getRootCategories_ShouldReturnOkWithRootCategories() throws Exception {
        List<CategoryResponse> rootCategories = List.of(rootCategoryResponse);
        when(categoryService.getRootCategories()).thenReturn(rootCategories);

        mockMvc.perform(get("/api/category/root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("root-id"))
                .andExpect(jsonPath("$[0].parentId").isEmpty());

        verify(categoryService).getRootCategories();
    }

    @Test
    void updateCategory_WithValidRequest_ShouldReturnOk() throws Exception {
        CategoryRequest updateRequest = new CategoryRequest(
                "Updated Category",
                "Updated Description",
                null
        );

        CategoryResponse updatedResponse = new CategoryResponse(
                "category-id-1",
                "Updated Category",
                "Updated Description",
                null,
                List.of(),
                categoryResponse.createdAt(),
                LocalDateTime.now()
        );

        when(categoryService.updateCategory(eq("category-id-1"), any(CategoryRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/category/category-id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("category-id-1"))
                .andExpect(jsonPath("$.name").value("Updated Category"))
                .andExpect(jsonPath("$.description").value("Updated Description"));

        verify(categoryService).updateCategory(eq("category-id-1"), any(CategoryRequest.class));
    }

    @Test
    void updateCategory_WithInvalidId_ShouldReturnNotFound() throws Exception {
        when(categoryService.updateCategory(eq("invalid-id"), any(CategoryRequest.class)))
                .thenThrow(new CategoryNotFoundException("Category not found with id: invalid-id"));

        mockMvc.perform(put("/api/category/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCategoryRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: invalid-id"));

        verify(categoryService).updateCategory(eq("invalid-id"), any(CategoryRequest.class));
    }

    @Test
    void updateCategory_WithCircularReference_ShouldReturnBadRequest() throws Exception {
        when(categoryService.updateCategory(eq("category-id-1"), any(CategoryRequest.class)))
                .thenThrow(new CategoryHierarchyException("Circular reference detected"));

        mockMvc.perform(put("/api/category/category-id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCategoryRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Circular reference detected"));

        verify(categoryService).updateCategory(eq("category-id-1"), any(CategoryRequest.class));
    }

    @Test
    void deleteCategory_WithValidId_ShouldReturnNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory("category-id-1");

        mockMvc.perform(delete("/api/category/category-id-1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory("category-id-1");
    }

    @Test
    void deleteCategory_WithInvalidId_ShouldReturnNotFound() throws Exception {
        doThrow(new CategoryNotFoundException("Category not found with id: invalid-id"))
                .when(categoryService).deleteCategory("invalid-id");

        mockMvc.perform(delete("/api/category/invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: invalid-id"));

        verify(categoryService).deleteCategory("invalid-id");
    }

    @Test
    void deleteCategory_WithAssignedProducts_ShouldReturnConflict() throws Exception {
        doThrow(new CategoryDeletionException("Cannot delete category with assigned products"))
                .when(categoryService).deleteCategory("category-id-1");

        mockMvc.perform(delete("/api/category/category-id-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete category with assigned products"));

        verify(categoryService).deleteCategory("category-id-1");
    }

    @Test
    void createCategory_WithInvalidNameFormat_ShouldReturnBadRequest() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Invalid@Name!", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategory_WithInvalidParentIdFormat_ShouldReturnBadRequest() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", "Description", "invalid@parent!");

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.parentId").exists());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    void updateCategory_WithInvalidNameFormat_ShouldReturnBadRequest() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Invalid@Name!", "Description", null);

        mockMvc.perform(put("/api/category/category-id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists());

        verify(categoryService, never()).updateCategory(anyString(), any(CategoryRequest.class));
    }

    @Test
    void createCategory_WithHierarchyException_ShouldReturnBadRequest() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new CategoryHierarchyException("Circular reference detected in category hierarchy"));

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCategoryRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Circular reference detected in category hierarchy"));
    }

    @Test
    void getAllCategories_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        when(categoryService.getAllCategories())
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/category"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}