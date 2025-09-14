package com.PetStore.product.integration;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Basic integration test to verify category-product workflow functionality.
 * This test serves as a foundation for the comprehensive workflow tests.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class BasicCategoryProductIntegrationTest {

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
    void basicWorkflow_CreateCategoryAndProduct_ShouldWork() throws Exception {
        // Step 1: Create a category
        CategoryRequest categoryRequest = new CategoryRequest("Test Category", "Test Description", null);
        
        MvcResult categoryResult = mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Category"))
                .andReturn();

        CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(), 
                CategoryResponse.class
        );

        // Step 2: Create a product assigned to the category
        ProductRequest productRequest = new ProductRequest(
                null, 
                "Test Product", 
                "Test Description", 
                "TEST-001", 
                new BigDecimal("19.99"),
                List.of(category.id())
        );
        
        MvcResult productResult = mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.categoryIds[0]").value(category.id()))
                .andReturn();

        ProductResponse product = objectMapper.readValue(
                productResult.getResponse().getContentAsString(), 
                ProductResponse.class
        );

        // Step 3: Verify product filtering by category
        MvcResult filterResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", category.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> filteredProducts = objectMapper.readValue(
                filterResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );

        assertThat(filteredProducts).hasSize(1);
        assertThat(filteredProducts.get(0).id()).isEqualTo(product.id());
        assertThat(filteredProducts.get(0).categoryIds()).contains(category.id());

        // Step 4: Verify category cannot be deleted with assigned products
        mockMvc.perform(delete("/api/category/" + category.id()))
                .andExpect(status().isConflict());

        // Step 5: Verify category still exists
        mockMvc.perform(get("/api/category/" + category.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.id()));
    }

    @Test
    void categoryHierarchy_CreateParentAndChild_ShouldWork() throws Exception {
        // Create parent category
        CategoryRequest parentRequest = new CategoryRequest("Parent Category", "Parent Description", null);
        
        MvcResult parentResult = mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponse parent = objectMapper.readValue(
                parentResult.getResponse().getContentAsString(), 
                CategoryResponse.class
        );

        // Create child category
        CategoryRequest childRequest = new CategoryRequest("Child Category", "Child Description", parent.id());
        
        MvcResult childResult = mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parent.id()))
                .andReturn();

        CategoryResponse child = objectMapper.readValue(
                childResult.getResponse().getContentAsString(), 
                CategoryResponse.class
        );

        // Verify parent-child relationship
        mockMvc.perform(get("/api/category/" + parent.id() + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(child.id()));

        // Verify category tree
        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // One root category
    }
}