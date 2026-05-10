package com.PetStore.product.service;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.dto.CategoryTreeResponse;
import com.PetStore.product.exception.CategoryDeletionException;
import com.PetStore.product.exception.CategoryHierarchyException;
import com.PetStore.product.exception.CategoryNotFoundException;
import com.PetStore.product.exception.CategoryValidationException;
import com.PetStore.product.model.Category;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;
    
    private static final int MAX_HIERARCHY_DEPTH = 10;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        log.info("Creating category with name: {}", categoryRequest.name());

        // Validate input parameters
        validateCategoryRequest(categoryRequest);

        // Validate category name uniqueness within the same parent level
        validateCategoryNameUniqueness(categoryRequest.name(), categoryRequest.parentId());

        // Validate parent category exists if parentId is provided
        Category parentCategory = null;
        if (categoryRequest.parentId() != null && !categoryRequest.parentId().trim().isEmpty()) {
            parentCategory = categoryRepository.findById(categoryRequest.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException(
                            "Parent category not found with id: " + categoryRequest.parentId()));
        }

        LocalDateTime now = LocalDateTime.now();
        Category category = Category.builder()
                .name(categoryRequest.name().trim())
                .description(categoryRequest.description() != null ? categoryRequest.description().trim() : null)
                .parentId(categoryRequest.parentId() != null && !categoryRequest.parentId().trim().isEmpty() 
                    ? categoryRequest.parentId().trim() : null)
                .childIds(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Save category and update parent atomically
        Category savedCategory = categoryRepository.save(category);

        // Update parent category's childIds atomically if parent exists
        if (parentCategory != null) {
            Query parentQuery = new Query(Criteria.where("_id").is(parentCategory.getId()));
            Update parentUpdate = new Update()
                    .push("childIds", savedCategory.getId())
                    .set("updatedAt", now);
            
            mongoTemplate.updateFirst(parentQuery, parentUpdate, Category.class);
            log.debug("Updated parent category {} with new child {}", parentCategory.getId(), savedCategory.getId());
        }

        log.info("Category created successfully with id: {}", savedCategory.getId());
        return mapToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(String id, CategoryRequest categoryRequest) {
        log.info("Updating category with id: {}", id);

        // Validate input parameters
        validateCategoryId(id);
        validateCategoryRequest(categoryRequest);

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        String trimmedName = categoryRequest.name().trim();
        String normalizedParentId = categoryRequest.parentId() != null && !categoryRequest.parentId().trim().isEmpty() 
            ? categoryRequest.parentId().trim() : null;

        // Validate category name uniqueness if name is being changed
        if (!existingCategory.getName().equals(trimmedName)) {
            validateCategoryNameUniqueness(trimmedName, normalizedParentId);
        }

        // Validate parent category change doesn't create circular reference
        if (!java.util.Objects.equals(normalizedParentId, existingCategory.getParentId())) {
            if (normalizedParentId != null) {
                validateNoCircularReference(id, normalizedParentId);

                // Verify parent category exists
                categoryRepository.findById(normalizedParentId)
                        .orElseThrow(() -> new CategoryNotFoundException(
                                "Parent category not found with id: " + normalizedParentId));
            }
        }

        // Handle parent category changes atomically
        handleParentCategoryChangeAtomically(existingCategory.getId(), normalizedParentId);

        // Update category fields
        Query categoryQuery = new Query(Criteria.where("_id").is(id));
        Update categoryUpdate = new Update()
                .set("name", trimmedName)
                .set("description", categoryRequest.description() != null ? categoryRequest.description().trim() : null)
                .set("parentId", normalizedParentId)
                .set("updatedAt", LocalDateTime.now());

        Category updatedCategory = mongoTemplate.findAndModify(categoryQuery, categoryUpdate, Category.class);
        if (updatedCategory == null) {
            throw new CategoryNotFoundException("Category not found with id: " + id);
        }

        log.info("Category updated successfully with id: {}", updatedCategory.getId());
        return mapToResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(String id) {
        log.info("Deleting category with id: {}", id);

        // Validate input parameter
        validateCategoryId(id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        // Check if category has assigned products
        if (hasAssignedProducts(id)) {
            long productCount = getAssignedProductCount(id);
            throw new CategoryDeletionException(
                String.format("Cannot delete category '%s' as it has %d assigned product(s). " +
                    "Please remove all products from this category before deletion.", 
                    category.getName(), productCount));
        }

        // Handle child categories - make them orphaned atomically
        if (!category.getChildIds().isEmpty()) {
            Query childrenQuery = new Query(Criteria.where("_id").in(category.getChildIds()));
            Update childrenUpdate = new Update()
                    .set("parentId", null)
                    .set("updatedAt", LocalDateTime.now());
            
            mongoTemplate.updateMulti(childrenQuery, childrenUpdate, Category.class);
            log.info("Orphaned {} child categories during deletion of category: {}", category.getChildIds().size(), id);
        }

        // Remove this category from parent's childIds atomically
        if (category.getParentId() != null) {
            Query parentQuery = new Query(Criteria.where("_id").is(category.getParentId()));
            Update parentUpdate = new Update()
                    .pull("childIds", id)
                    .set("updatedAt", LocalDateTime.now());
            
            mongoTemplate.updateFirst(parentQuery, parentUpdate, Category.class);
            log.debug("Removed category {} from parent {}", id, category.getParentId());
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }

    public CategoryResponse getCategoryById(String id) {
        log.info("Retrieving category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        return mapToResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        log.info("Retrieving all categories");

        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CategoryResponse> getRootCategories() {
        log.info("Retrieving root categories");

        return categoryRepository.findByParentIdIsNull()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CategoryResponse> getChildCategories(String parentId) {
        log.info("Retrieving child categories for parent id: {}", parentId);

        // Verify parent category exists
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new CategoryNotFoundException("Parent category not found with id: " + parentId));

        return categoryRepository.findByParentId(parentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CategoryTreeResponse> getCategoryTree() {
        log.info("Building category tree structure");

        // Get all root categories and build tree recursively
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();
        return rootCategories.stream()
                .map(this::buildCategoryTreeResponse)
                .toList();
    }

    private CategoryTreeResponse buildCategoryTreeResponse(Category category) {
        // Recursively build child categories
        List<Category> children = categoryRepository.findByParentId(category.getId());
        List<CategoryTreeResponse> childResponses = children.stream()
                .map(this::buildCategoryTreeResponse)
                .toList();

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParentId(),
                childResponses,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private void validateCategoryNameUniqueness(String name, String parentId) {
        boolean nameExists;
        if (parentId != null) {
            nameExists = categoryRepository.existsByNameAndParentId(name, parentId);
        } else {
            nameExists = categoryRepository.existsByNameAndParentIdIsNull(name);
        }

        if (nameExists) {
            throw new CategoryValidationException("Category name '" + name + "' already exists at this level");
        }
    }

    private void validateNoCircularReference(String categoryId, String newParentId) {
        if (categoryId.equals(newParentId)) {
            throw new CategoryHierarchyException("Category cannot be its own parent");
        }

        Set<String> visited = new HashSet<>();
        String currentParentId = newParentId;
        int depth = 0;

        // Batch fetch all categories in the potential hierarchy to reduce database queries
        List<String> idsToCheck = new ArrayList<>();
        String tempId = newParentId;
        while (tempId != null && depth < MAX_HIERARCHY_DEPTH) {
            idsToCheck.add(tempId);
            // Get next parent ID from cache if available, otherwise break and fetch batch
            Category tempCategory = categoryRepository.findById(tempId).orElse(null);
            tempId = tempCategory != null ? tempCategory.getParentId() : null;
            depth++;
        }

        // Check hierarchy depth limit
        if (depth >= MAX_HIERARCHY_DEPTH) {
            throw new CategoryHierarchyException(
                    String.format("Category hierarchy cannot exceed %d levels", MAX_HIERARCHY_DEPTH));
        }

        // Fetch all categories in batch
        Map<String, Category> categoryMap = categoryRepository.findAllById(idsToCheck)
                .stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, cat -> cat));

        // Now validate the hierarchy using cached data
        currentParentId = newParentId;
        depth = 0;
        while (currentParentId != null && depth < MAX_HIERARCHY_DEPTH) {
            if (visited.contains(currentParentId)) {
                throw new CategoryHierarchyException("Circular reference detected in category hierarchy");
            }

            if (currentParentId.equals(categoryId)) {
                throw new CategoryHierarchyException(
                        "Cannot set descendant as parent - would create circular reference");
            }

            visited.add(currentParentId);
            depth++;

            Category parentCategory = categoryMap.get(currentParentId);
            if (parentCategory == null) {
                throw new CategoryNotFoundException("Parent category not found with id: " + currentParentId);
            }
            currentParentId = parentCategory.getParentId();
        }
    }

    private void handleParentCategoryChangeAtomically(String categoryId, String newParentId) {
        // Get current category to find old parent
        Category currentCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + categoryId));
        
        String oldParentId = currentCategory.getParentId();
        LocalDateTime now = LocalDateTime.now();

        // If parent is not changing, do nothing
        if ((oldParentId == null && newParentId == null) ||
                (oldParentId != null && oldParentId.equals(newParentId))) {
            return;
        }

        // Remove from old parent's childIds atomically
        if (oldParentId != null) {
            Query oldParentQuery = new Query(Criteria.where("_id").is(oldParentId));
            Update oldParentUpdate = new Update()
                    .pull("childIds", categoryId)
                    .set("updatedAt", now);
            
            mongoTemplate.updateFirst(oldParentQuery, oldParentUpdate, Category.class);
            log.debug("Removed category {} from old parent {}", categoryId, oldParentId);
        }

        // Add to new parent's childIds atomically
        if (newParentId != null) {
            Query newParentQuery = new Query(Criteria.where("_id").is(newParentId));
            Update newParentUpdate = new Update()
                    .addToSet("childIds", categoryId)
                    .set("updatedAt", now);
            
            mongoTemplate.updateFirst(newParentQuery, newParentUpdate, Category.class);
            log.debug("Added category {} to new parent {}", categoryId, newParentId);
        }
    }

    
    private boolean hasAssignedProducts(String categoryId) {
        // Use count query instead of loading all products
        Query query = new Query(Criteria.where("categoryIds").is(categoryId));
        long count = mongoTemplate.count(query, "product");
        return count > 0;
    }

    private long getAssignedProductCount(String categoryId) {
        // Get actual count for error messages
        Query query = new Query(Criteria.where("categoryIds").is(categoryId));
        return mongoTemplate.count(query, "product");
    }

    private void validateCategoryRequest(CategoryRequest categoryRequest) {
        if (categoryRequest == null) {
            throw new CategoryValidationException("Category request cannot be null");
        }
        
        // Additional business logic validation that's not covered by annotations
        // Format validation is handled by @Pattern annotations in DTO
        // Length validation is handled by @Size annotations in DTO
        // Required field validation is handled by @NotBlank annotations in DTO
    }

    private void validateCategoryId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new CategoryValidationException("Category ID is required");
        }
        
        String trimmedId = id.trim();
        if (!trimmedId.matches("^[a-zA-Z0-9\\-_]+$")) {
            throw new CategoryValidationException("Category ID must be a valid identifier");
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParentId(),
                category.getChildIds(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}