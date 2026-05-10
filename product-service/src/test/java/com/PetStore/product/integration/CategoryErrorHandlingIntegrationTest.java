package com.PetStore.product.integration;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.model.Category;
import com.PetStore.product.model.Product;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class CategoryErrorHandlingIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        categoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void createCategory_WithInvalidName_ShouldReturnValidationError() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    void createCategory_WithInvalidNameFormat_ShouldReturnValidationError() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Invalid@Name!", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    void createCategory_WithInvalidParentIdFormat_ShouldReturnValidationError() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", "Description", "invalid@parent!");

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.parentId").exists());
    }

    @Test
    void createCategory_WithNonExistentParent_ShouldReturnNotFound() throws Exception {
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", "Description", "non-existent-parent");

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Parent category not found with id: non-existent-parent"));
    }

    @Test
    void createCategory_WithDuplicateName_ShouldReturnBadRequest() throws Exception {
        // Create first category
        Category existingCategory = Category.builder()
                .name("Existing Category")
                .description("Description")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        categoryRepository.save(existingCategory);

        // Try to create duplicate
        CategoryRequest duplicateRequest = new CategoryRequest("Existing Category", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Category name 'Existing Category' already exists at this level"));
    }

    @Test
    void updateCategory_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        CategoryRequest updateRequest = new CategoryRequest("Updated Name", "Updated Description", null);

        mockMvc.perform(put("/api/category/non-existent-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: non-existent-id"));
    }

    @Test
    void updateCategory_WithCircularReference_ShouldReturnBadRequest() throws Exception {
        // Create parent category
        Category parentCategory = Category.builder()
                .name("Parent Category")
                .description("Parent Description")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Category savedParent = categoryRepository.save(parentCategory);

        // Create child category
        Category childCategory = Category.builder()
                .name("Child Category")
                .description("Child Description")
                .parentId(savedParent.getId())
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Category savedChild = categoryRepository.save(childCategory);

        // Update parent's childIds
        savedParent.getChildIds().add(savedChild.getId());
        categoryRepository.save(savedParent);

        // Try to make parent a child of its own child (circular reference)
        CategoryRequest circularRequest = new CategoryRequest("Parent Category", "Parent Description", savedChild.getId());

        mockMvc.perform(put("/api/category/" + savedParent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(circularRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot set descendant as parent - would create circular reference"));
    }

    @Test
    void deleteCategory_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/category/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: non-existent-id"));
    }

    @Test
    void deleteCategory_WithAssignedProducts_ShouldReturnConflict() throws Exception {
        // Create category
        Category category = Category.builder()
                .name("Category with Products")
                .description("Description")
                .parentId(null)
                .childIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Category savedCategory = categoryRepository.save(category);

        // Create product assigned to category
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .skuCode("TEST-001")
                .price(BigDecimal.valueOf(19.99))
                .categoryIds(List.of(savedCategory.getId()))
                .build();
        productRepository.save(product);

        mockMvc.perform(delete("/api/category/" + savedCategory.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete category 'Category with Products' as it has 1 assigned product(s). Please remove all products from this category before deletion."));
    }

    @Test
    void getCategoryById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/category/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id: non-existent-id"));
    }

    @Test
    void getChildCategories_WithNonExistentParentId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/category/non-existent-parent/children"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Parent category not found with id: non-existent-parent"));
    }

    @Test
    void createCategory_WithValidNameContainingSpecialCharacters_ShouldSucceed() throws Exception {
        CategoryRequest validRequest = new CategoryRequest("Dogs & Cats-Toys_2024", "Description", null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dogs & Cats-Toys_2024"));
    }

    @Test
    void createCategory_WithLongDescription_ShouldReturnValidationError() throws Exception {
        String longDescription = "A".repeat(501);
        CategoryRequest invalidRequest = new CategoryRequest("Valid Name", longDescription, null);

        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.description").exists());
    }
}