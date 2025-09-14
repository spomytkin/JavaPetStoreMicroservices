package com.PetStore.product.controller;

import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductRequest validProductRequest;
    private ProductResponse productResponse1;
    private ProductResponse productResponse2;
    private ProductResponse productWithCategoriesResponse;

    @BeforeEach
    void setUp() {
        validProductRequest = new ProductRequest(
                null, // id is null for creation
                "Test Product",
                "Test Description",
                "TEST-001",
                new BigDecimal("29.99"),
                List.of("category-1", "category-2")
        );

        productResponse1 = new ProductResponse(
                "product-id-1",
                "Test Product 1",
                "Test Description 1",
                "TEST-001",
                new BigDecimal("29.99"),
                List.of("category-1")
        );

        productResponse2 = new ProductResponse(
                "product-id-2",
                "Test Product 2",
                "Test Description 2",
                "TEST-002",
                new BigDecimal("39.99"),
                List.of("category-2")
        );

        productWithCategoriesResponse = new ProductResponse(
                "product-id-3",
                "Multi-Category Product",
                "Product in multiple categories",
                "TEST-003",
                new BigDecimal("49.99"),
                List.of("category-1", "category-2")
        );
    }

    @Test
    void createProduct_WithValidRequest_ShouldReturnCreated() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(productResponse1);

        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("product-id-1"))
                .andExpect(jsonPath("$.name").value("Test Product 1"))
                .andExpect(jsonPath("$.description").value("Test Description 1"))
                .andExpect(jsonPath("$.skuCode").value("TEST-001"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.categoryIds.length()").value(1))
                .andExpect(jsonPath("$.categoryIds[0]").value("category-1"));

        verify(productService).createProduct(any(ProductRequest.class));
    }

    @Test
    void getAllProducts_WithoutFilters_ShouldReturnAllProducts() throws Exception {
        List<ProductResponse> allProducts = Arrays.asList(productResponse1, productResponse2, productWithCategoriesResponse);
        when(productService.getAllProducts()).thenReturn(allProducts);

        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("product-id-1"))
                .andExpect(jsonPath("$[1].id").value("product-id-2"))
                .andExpect(jsonPath("$[2].id").value("product-id-3"));

        verify(productService).getAllProducts();
        verify(productService, never()).getProductsByCategory(anyString());
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithCategoryFilter_ShouldReturnFilteredProducts() throws Exception {
        List<ProductResponse> filteredProducts = Arrays.asList(productResponse1, productWithCategoriesResponse);
        when(productService.getProductsByCategory("category-1")).thenReturn(filteredProducts);

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "category-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("product-id-1"))
                .andExpect(jsonPath("$[1].id").value("product-id-3"));

        verify(productService).getProductsByCategory("category-1");
        verify(productService, never()).getAllProducts();
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithCategoryFilterAndIncludeSubcategories_ShouldReturnHierarchyProducts() throws Exception {
        List<ProductResponse> hierarchyProducts = Arrays.asList(productResponse1, productResponse2, productWithCategoriesResponse);
        when(productService.getProductsByCategoryHierarchy("category-1")).thenReturn(hierarchyProducts);

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "category-1")
                        .param("includeSubcategories", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("product-id-1"))
                .andExpect(jsonPath("$[1].id").value("product-id-2"))
                .andExpect(jsonPath("$[2].id").value("product-id-3"));

        verify(productService).getProductsByCategoryHierarchy("category-1");
        verify(productService, never()).getAllProducts();
        verify(productService, never()).getProductsByCategory(anyString());
    }

    @Test
    void getAllProducts_WithEmptyCategoryId_ShouldReturnAllProducts() throws Exception {
        List<ProductResponse> allProducts = Arrays.asList(productResponse1, productResponse2);
        when(productService.getAllProducts()).thenReturn(allProducts);

        mockMvc.perform(get("/api/product")
                        .param("categoryId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(productService).getAllProducts();
        verify(productService, never()).getProductsByCategory(anyString());
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithWhitespaceCategoryId_ShouldReturnAllProducts() throws Exception {
        List<ProductResponse> allProducts = Arrays.asList(productResponse1, productResponse2);
        when(productService.getAllProducts()).thenReturn(allProducts);

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(productService).getAllProducts();
        verify(productService, never()).getProductsByCategory(anyString());
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithInvalidCategoryId_ShouldReturnNotFound() throws Exception {
        when(productService.getProductsByCategory("invalid-category"))
                .thenThrow(new CategoryNotFoundException("Category not found with id: invalid-category"));

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "invalid-category"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: invalid-category"));

        verify(productService).getProductsByCategory("invalid-category");
        verify(productService, never()).getAllProducts();
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithInvalidCategoryIdAndIncludeSubcategories_ShouldReturnNotFound() throws Exception {
        when(productService.getProductsByCategoryHierarchy("invalid-category"))
                .thenThrow(new CategoryNotFoundException("Category not found with id: invalid-category"));

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "invalid-category")
                        .param("includeSubcategories", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: invalid-category"));

        verify(productService).getProductsByCategoryHierarchy("invalid-category");
        verify(productService, never()).getAllProducts();
        verify(productService, never()).getProductsByCategory(anyString());
    }

    @Test
    void getAllProducts_WithIncludeSubcategoriesFalse_ShouldUseDirectCategoryFilter() throws Exception {
        List<ProductResponse> filteredProducts = Arrays.asList(productResponse1);
        when(productService.getProductsByCategory("category-1")).thenReturn(filteredProducts);

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "category-1")
                        .param("includeSubcategories", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("product-id-1"));

        verify(productService).getProductsByCategory("category-1");
        verify(productService, never()).getAllProducts();
        verify(productService, never()).getProductsByCategoryHierarchy(anyString());
    }

    @Test
    void getAllProducts_WithCategoryFilterReturningEmptyList_ShouldReturnEmptyArray() throws Exception {
        when(productService.getProductsByCategory("empty-category")).thenReturn(List.of());

        mockMvc.perform(get("/api/product")
                        .param("categoryId", "empty-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService).getProductsByCategory("empty-category");
    }
}