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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for the complete category-product workflow.
 * Tests end-to-end functionality including category creation, hierarchy management,
 * product assignment, filtering, and data integrity constraints.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class CategoryProductWorkflowIntegrationTest {

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
    void completeWorkflow_CreateCategoryHierarchyAndAssignProducts_ShouldWorkEndToEnd() throws Exception {
        // Step 1: Create root categories
        CategoryResponse petsCategory = createCategory("Pets", "All pet products", null);
        CategoryResponse accessoriesCategory = createCategory("Accessories", "Pet accessories", null);

        // Step 2: Create subcategories under Pets
        CategoryResponse dogsCategory = createCategory("Dogs", "Products for dogs", petsCategory.id());
        CategoryResponse catsCategory = createCategory("Cats", "Products for cats", petsCategory.id());

        // Step 3: Create deeper hierarchy under Dogs
        CategoryResponse dogFoodCategory = createCategory("Dog Food", "Food for dogs", dogsCategory.id());
        CategoryResponse dogToysCategory = createCategory("Dog Toys", "Toys for dogs", dogsCategory.id());

        // Step 4: Create products and assign to categories
        ProductResponse dogFoodProduct = createProduct(
                "Premium Dog Food", 
                "High-quality dry food for adult dogs", 
                "DF-001", 
                new BigDecimal("45.99"),
                List.of(dogFoodCategory.id(), petsCategory.id())
        );

        ProductResponse dogToyProduct = createProduct(
                "Interactive Dog Toy", 
                "Puzzle toy for mental stimulation", 
                "DT-001", 
                new BigDecimal("19.99"),
                List.of(dogToysCategory.id(), dogsCategory.id())
        );

        ProductResponse catFoodProduct = createProduct(
                "Premium Cat Food", 
                "Nutritious wet food for cats", 
                "CF-001", 
                new BigDecimal("32.99"),
                List.of(catsCategory.id(), petsCategory.id())
        );

        ProductResponse collarProduct = createProduct(
                "Adjustable Pet Collar", 
                "Universal collar for dogs and cats", 
                "AC-001", 
                new BigDecimal("15.99"),
                List.of(accessoriesCategory.id(), dogsCategory.id(), catsCategory.id())
        );

        // Step 5: Verify category hierarchy structure
        verifyCategoryHierarchy(petsCategory, dogsCategory, catsCategory, dogFoodCategory, dogToysCategory);

        // Step 6: Test product filtering by categories
        testProductFilteringByCategory(petsCategory, dogsCategory, dogFoodCategory, 
                dogFoodProduct, dogToyProduct, catFoodProduct, collarProduct);

        // Step 7: Test category tree retrieval
        testCategoryTreeRetrieval(petsCategory, dogsCategory, catsCategory, dogFoodCategory, dogToysCategory);

        // Step 8: Test data integrity constraints
        testDataIntegrityConstraints(dogFoodCategory, dogFoodProduct);

        // Step 9: Test category hierarchy operations
        testCategoryHierarchyOperations(dogsCategory, dogFoodCategory);
    }

    private CategoryResponse createCategory(String name, String description, String parentId) throws Exception {
        CategoryRequest request = new CategoryRequest(name, description, parentId);
        
        MvcResult result = mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.parentId").value(parentId))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    private ProductResponse createProduct(String name, String description, String skuCode, 
                                        BigDecimal price, List<String> categoryIds) throws Exception {
        ProductRequest request = new ProductRequest(null, name, description, skuCode, price, categoryIds);
        
        MvcResult result = mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.skuCode").value(skuCode))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), ProductResponse.class);
    }

    private void verifyCategoryHierarchy(CategoryResponse petsCategory, CategoryResponse dogsCategory, 
                                       CategoryResponse catsCategory, CategoryResponse dogFoodCategory, 
                                       CategoryResponse dogToysCategory) throws Exception {
        // Verify parent-child relationships
        CategoryResponse updatedPetsCategory = getCategoryById(petsCategory.id());
        assertThat(updatedPetsCategory.childIds()).containsExactlyInAnyOrder(dogsCategory.id(), catsCategory.id());

        CategoryResponse updatedDogsCategory = getCategoryById(dogsCategory.id());
        assertThat(updatedDogsCategory.parentId()).isEqualTo(petsCategory.id());
        assertThat(updatedDogsCategory.childIds()).containsExactlyInAnyOrder(dogFoodCategory.id(), dogToysCategory.id());

        CategoryResponse updatedCatsCategory = getCategoryById(catsCategory.id());
        assertThat(updatedCatsCategory.parentId()).isEqualTo(petsCategory.id());
        assertThat(updatedCatsCategory.childIds()).isEmpty();

        // Verify child categories
        mockMvc.perform(get("/api/category/" + petsCategory.id() + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(
                        dogsCategory.id(), catsCategory.id())));
    }

    private void testProductFilteringByCategory(CategoryResponse petsCategory, CategoryResponse dogsCategory, 
                                              CategoryResponse dogFoodCategory, ProductResponse dogFoodProduct, 
                                              ProductResponse dogToyProduct, ProductResponse catFoodProduct, 
                                              ProductResponse collarProduct) throws Exception {
        // Test filtering by root category (should include all pet products)
        MvcResult petsResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", petsCategory.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> petsProducts = objectMapper.readValue(
                petsResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(petsProducts).hasSize(3);
        assertThat(petsProducts).extracting(ProductResponse::id)
                .containsExactlyInAnyOrder(dogFoodProduct.id(), catFoodProduct.id(), collarProduct.id());

        // Test filtering by dogs category (should include dog products and collar)
        MvcResult dogsResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", dogsCategory.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> dogsProducts = objectMapper.readValue(
                dogsResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(dogsProducts).hasSize(2);
        assertThat(dogsProducts).extracting(ProductResponse::id)
                .containsExactlyInAnyOrder(dogToyProduct.id(), collarProduct.id());

        // Test filtering by specific dog food category
        MvcResult dogFoodResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", dogFoodCategory.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> dogFoodProducts = objectMapper.readValue(
                dogFoodResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(dogFoodProducts).hasSize(1);
        assertThat(dogFoodProducts.get(0).id()).isEqualTo(dogFoodProduct.id());

        // Test filtering with includeSubcategories parameter
        MvcResult hierarchyResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", dogsCategory.id())
                        .param("includeSubcategories", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> hierarchyProducts = objectMapper.readValue(
                hierarchyResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(hierarchyProducts).hasSize(3); // dog toy + collar + dog food
        assertThat(hierarchyProducts).extracting(ProductResponse::id)
                .containsExactlyInAnyOrder(dogToyProduct.id(), collarProduct.id(), dogFoodProduct.id());
    }

    private void testCategoryTreeRetrieval(CategoryResponse petsCategory, CategoryResponse dogsCategory, 
                                         CategoryResponse catsCategory, CategoryResponse dogFoodCategory, 
                                         CategoryResponse dogToysCategory) throws Exception {
        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Pets and Accessories root categories
                .andExpect(jsonPath("$[?(@.name=='Pets')].children.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='Pets')].children[?(@.name=='Dogs')].children.length()").value(2));
    }

    private void testDataIntegrityConstraints(CategoryResponse dogFoodCategory, ProductResponse dogFoodProduct) throws Exception {
        // Test that category with assigned products cannot be deleted
        mockMvc.perform(delete("/api/category/" + dogFoodCategory.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete category")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned product")));

        // Verify category still exists
        mockMvc.perform(get("/api/category/" + dogFoodCategory.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dogFoodCategory.id()));

        // Verify product still exists and has category assigned
        mockMvc.perform(get("/api/product/" + dogFoodProduct.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryIds").value(org.hamcrest.Matchers.hasItem(dogFoodCategory.id())));
    }

    private void testCategoryHierarchyOperations(CategoryResponse dogsCategory, CategoryResponse dogFoodCategory) throws Exception {
        // Test circular reference prevention
        CategoryRequest circularRequest = new CategoryRequest("Dogs", "Updated description", dogFoodCategory.id());
        
        mockMvc.perform(put("/api/category/" + dogsCategory.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(circularRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("circular reference")));

        // Test valid category update
        CategoryRequest validUpdate = new CategoryRequest("Dogs Updated", "Updated description for dogs", dogsCategory.parentId());
        
        mockMvc.perform(put("/api/category/" + dogsCategory.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dogs Updated"))
                .andExpect(jsonPath("$.description").value("Updated description for dogs"));
    }

    @Test
    void categoryDeletion_WithoutAssignedProducts_ShouldSucceedAndUpdateHierarchy() throws Exception {
        // Create category hierarchy
        CategoryResponse parentCategory = createCategory("Parent", "Parent category", null);
        CategoryResponse childCategory = createCategory("Child", "Child category", parentCategory.id());
        CategoryResponse grandchildCategory = createCategory("Grandchild", "Grandchild category", childCategory.id());

        // Delete child category (no products assigned)
        mockMvc.perform(delete("/api/category/" + childCategory.id()))
                .andExpect(status().isNoContent());

        // Verify child category is deleted
        mockMvc.perform(get("/api/category/" + childCategory.id()))
                .andExpect(status().isNotFound());

        // Verify grandchild category becomes orphaned (parentId set to null)
        CategoryResponse updatedGrandchild = getCategoryById(grandchildCategory.id());
        assertThat(updatedGrandchild.parentId()).isNull();

        // Verify parent category's childIds are updated
        CategoryResponse updatedParent = getCategoryById(parentCategory.id());
        assertThat(updatedParent.childIds()).doesNotContain(childCategory.id());
    }

    @Test
    void productCategoryAssignment_MultipleOperations_ShouldMaintainConsistency() throws Exception {
        // Create categories
        CategoryResponse category1 = createCategory("Category 1", "First category", null);
        CategoryResponse category2 = createCategory("Category 2", "Second category", null);
        CategoryResponse category3 = createCategory("Category 3", "Third category", null);

        // Create product with initial categories
        ProductResponse product = createProduct(
                "Test Product", 
                "Test description", 
                "TEST-001", 
                new BigDecimal("29.99"),
                List.of(category1.id(), category2.id())
        );

        // Update product to change category assignments
        ProductRequest updateRequest = new ProductRequest(
                product.id(),
                "Updated Product",
                "Updated description",
                "TEST-001",
                new BigDecimal("35.99"),
                List.of(category2.id(), category3.id()) // Remove category1, add category3
        );

        mockMvc.perform(put("/api/product/" + product.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryIds").value(org.hamcrest.Matchers.containsInAnyOrder(
                        category2.id(), category3.id())));

        // Verify product filtering reflects the changes
        // Should not appear in category1 products
        MvcResult category1Result = mockMvc.perform(get("/api/product")
                        .param("categoryId", category1.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> category1Products = objectMapper.readValue(
                category1Result.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(category1Products).isEmpty();

        // Should appear in category2 and category3 products
        MvcResult category2Result = mockMvc.perform(get("/api/product")
                        .param("categoryId", category2.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> category2Products = objectMapper.readValue(
                category2Result.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(category2Products).hasSize(1);
        assertThat(category2Products.get(0).id()).isEqualTo(product.id());
    }

    @Test
    void largeHierarchy_PerformanceAndConsistency_ShouldHandleComplexStructures() throws Exception {
        // Create a complex hierarchy: Root -> 3 Level1 -> 2 Level2 each -> 2 Level3 each
        CategoryResponse root = createCategory("Root", "Root category", null);
        
        List<CategoryResponse> level1Categories = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            level1Categories.add(createCategory("Level1-" + i, "Level 1 category " + i, root.id()));
        }

        List<CategoryResponse> level2Categories = new ArrayList<>();
        for (CategoryResponse level1 : level1Categories) {
            for (int j = 1; j <= 2; j++) {
                level2Categories.add(createCategory("Level2-" + level1.name() + "-" + j, 
                        "Level 2 category", level1.id()));
            }
        }

        List<CategoryResponse> level3Categories = new ArrayList<>();
        for (CategoryResponse level2 : level2Categories) {
            for (int k = 1; k <= 2; k++) {
                level3Categories.add(createCategory("Level3-" + level2.name() + "-" + k, 
                        "Level 3 category", level2.id()));
            }
        }

        // Create products at different levels
        List<ProductResponse> products = new ArrayList<>();
        for (int i = 0; i < level3Categories.size(); i++) {
            CategoryResponse category = level3Categories.get(i);
            products.add(createProduct(
                    "Product-" + i, 
                    "Product description " + i, 
                    "PROD-" + String.format("%03d", i), 
                    new BigDecimal("19.99"),
                    List.of(category.id(), root.id())
            ));
        }

        // Verify hierarchy structure
        CategoryResponse updatedRoot = getCategoryById(root.id());
        assertThat(updatedRoot.childIds()).hasSize(3);

        // Test filtering by root category (should include all products)
        MvcResult rootResult = mockMvc.perform(get("/api/product")
                        .param("categoryId", root.id()))
                .andExpect(status().isOk())
                .andReturn();
        
        List<ProductResponse> rootProducts = objectMapper.readValue(
                rootResult.getResponse().getContentAsString(), 
                new TypeReference<List<ProductResponse>>() {}
        );
        assertThat(rootProducts).hasSize(products.size());

        // Test category tree retrieval for complex structure
        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Root')].children.length()").value(3));
    }

    private CategoryResponse getCategoryById(String categoryId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/category/" + categoryId))
                .andExpect(status().isOk())
                .andReturn();
        
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }
}