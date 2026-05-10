package com.PetStore.product.service;

import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.model.Product;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating product with name: {} and categories: {}", 
                productRequest.name(), productRequest.categoryIds());
        
        // Validate that all provided category IDs exist
        if (productRequest.categoryIds() != null && !productRequest.categoryIds().isEmpty()) {
            validateCategoryIds(productRequest.categoryIds());
        }
        
        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .skuCode(productRequest.skuCode())
                .price(productRequest.price())
                .categoryIds(productRequest.categoryIds() != null ? 
                    new ArrayList<>(productRequest.categoryIds()) : new ArrayList<>())
                .build();
        
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        
        return mapToResponse(savedProduct);
    }
    
    @Tool ("Returns the list of all products")
    public List<ProductResponse> getAllProducts() {
        log.info("Retrieving all products");
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    /**
     * Get products by a specific category ID
     * @param categoryId the category ID to filter by
     * @return list of products in the specified category
     */
    public List<ProductResponse> getProductsByCategory(String categoryId) {
        log.info("Retrieving products for category: {}", categoryId);
        
        // Validate that category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException("Category not found with id: " + categoryId);
        }
        
        return productRepository.findByCategoryIdsContaining(categoryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    /**
     * Get products by category hierarchy (includes products from subcategories)
     * @param categoryId the parent category ID
     * @return list of products in the category and all its subcategories
     */
    public List<ProductResponse> getProductsByCategoryHierarchy(String categoryId) {
        log.info("Retrieving products for category hierarchy starting from: {}", categoryId);
        
        // Validate that category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException("Category not found with id: " + categoryId);
        }
        
        // Get all category IDs in the hierarchy (current category + all descendants)
        Set<String> allCategoryIds = getAllDescendantCategoryIds(categoryId);
        allCategoryIds.add(categoryId); // Include the parent category itself
        
        return productRepository.findByCategoryIdsIn(new ArrayList<>(allCategoryIds))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    /**
     * Validate that all provided category IDs exist
     * @param categoryIds list of category IDs to validate
     * @throws CategoryNotFoundException if any category ID doesn't exist
     */
    private void validateCategoryIds(List<String> categoryIds) {
        for (String categoryId : categoryIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new CategoryNotFoundException("Category not found with id: " + categoryId);
            }
        }
    }
    
    /**
     * Get all descendant category IDs for a given parent category
     * @param parentCategoryId the parent category ID
     * @return set of all descendant category IDs
     */
    private Set<String> getAllDescendantCategoryIds(String parentCategoryId) {
        Set<String> descendants = new HashSet<>();
        
        // Get direct children
        List<String> directChildren = categoryRepository.findByParentId(parentCategoryId)
                .stream()
                .map(category -> category.getId())
                .toList();
        
        // Recursively get descendants of each child
        for (String childId : directChildren) {
            descendants.add(childId);
            descendants.addAll(getAllDescendantCategoryIds(childId));
        }
        
        return descendants;
    }
    
    /**
     * Map Product entity to ProductResponse DTO
     * @param product the product entity
     * @return ProductResponse DTO
     */
    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSkuCode(),
                product.getPrice(),
                product.getCategoryIds()
        );
    }
}
