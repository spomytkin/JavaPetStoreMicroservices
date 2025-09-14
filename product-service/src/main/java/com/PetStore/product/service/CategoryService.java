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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

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

        Category savedCategory = categoryRepository.save(category);

        // Update parent category's childIds if parent exists
        if (parentCategory != null) {
            parentCategory.getChildIds().add(savedCategory.getId());
            parentCategory.setUpdatedAt(now);
            categoryRepository.save(parentCategory);
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

        // Handle parent category changes
        handleParentCategoryChange(existingCategory, normalizedParentId);

        // Update category fields
        existingCategory.setName(trimmedName);
        existingCategory.setDescription(categoryRequest.description() != null ? categoryRequest.description().trim() : null);
        existingCategory.setParentId(normalizedParentId);
        existingCategory.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(existingCategory);

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
            long productCount = productRepository.findByCategoryIdsContaining(id).size();
            throw new CategoryDeletionException(
                String.format("Cannot delete category '%s' as it has %d assigned product(s). " +
                    "Please remove all products from this category before deletion.", 
                    category.getName(), productCount));
        }

        // Handle child categories - make them orphaned
        if (!category.getChildIds().isEmpty()) {
            List<Category> childCategories = categoryRepository.findAllById(category.getChildIds());
            for (Category child : childCategories) {
                child.setParentId(null);
                child.setUpdatedAt(LocalDateTime.now());
            }
            categoryRepository.saveAll(childCategories);
            log.info("Orphaned {} child categories during deletion of category: {}", childCategories.size(), id);
        }

        // Remove this category from parent's childIds
        if (category.getParentId() != null) {
            categoryRepository.findById(category.getParentId()).ifPresent(parent -> {
                parent.getChildIds().remove(id);
                parent.setUpdatedAt(LocalDateTime.now());
                categoryRepository.save(parent);
            });
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

    public List<CategoryResponse> getCategoryTree() {
        log.info("Building category tree structure");

        // Get all root categories and build tree recursively
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();
        return rootCategories.stream()
                .map(this::buildCategoryTree)
                .toList();
    }

    private CategoryResponse buildCategoryTree(Category category) {
        CategoryResponse response = mapToResponse(category);

        // Recursively build child categories
        List<Category> children = categoryRepository.findByParentId(category.getId());
        List<CategoryResponse> childResponses = children.stream()
                .map(this::buildCategoryTree)
                .toList();

        // Note: CategoryResponse record doesn't support setting children directly
        // This will be enhanced when CategoryTreeResponse is implemented in task 3
        return response;
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

        while (currentParentId != null) {
            if (visited.contains(currentParentId)) {
                throw new CategoryHierarchyException("Circular reference detected in category hierarchy");
            }

            if (currentParentId.equals(categoryId)) {
                throw new CategoryHierarchyException(
                        "Cannot set descendant as parent - would create circular reference");
            }

            visited.add(currentParentId);

            Category parentCategory = categoryRepository.findById(currentParentId).orElse(null);
            currentParentId = parentCategory != null ? parentCategory.getParentId() : null;
        }
    }

    private void handleParentCategoryChange(Category category, String newParentId) {
        String oldParentId = category.getParentId();

        // If parent is not changing, do nothing
        if ((oldParentId == null && newParentId == null) ||
                (oldParentId != null && oldParentId.equals(newParentId))) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Remove from old parent's childIds
        if (oldParentId != null) {
            categoryRepository.findById(oldParentId).ifPresent(oldParent -> {
                oldParent.getChildIds().remove(category.getId());
                oldParent.setUpdatedAt(now);
                categoryRepository.save(oldParent);
            });
        }

        // Add to new parent's childIds
        if (newParentId != null) {
            categoryRepository.findById(newParentId).ifPresent(newParent -> {
                if (!newParent.getChildIds().contains(category.getId())) {
                    newParent.getChildIds().add(category.getId());
                    newParent.setUpdatedAt(now);
                    categoryRepository.save(newParent);
                }
            });
        }
    }

    private boolean hasAssignedProducts(String categoryId) {
        // Check if any products are assigned to this category
        return !productRepository.findByCategoryIdsContaining(categoryId).isEmpty();
    }

    private void validateCategoryRequest(CategoryRequest categoryRequest) {
        if (categoryRequest == null) {
            throw new CategoryValidationException("Category request cannot be null");
        }
        
        if (categoryRequest.name() == null || categoryRequest.name().trim().isEmpty()) {
            throw new CategoryValidationException("Category name is required");
        }
        
        String trimmedName = categoryRequest.name().trim();
        if (trimmedName.length() < 2) {
            throw new CategoryValidationException("Category name must be at least 2 characters long");
        }
        
        if (trimmedName.length() > 100) {
            throw new CategoryValidationException("Category name cannot exceed 100 characters");
        }
        
        // Validate name format
        if (!trimmedName.matches("^[a-zA-Z0-9\\s\\-_&]+$")) {
            throw new CategoryValidationException(
                "Category name can only contain letters, numbers, spaces, hyphens, underscores, and ampersands");
        }
        
        // Validate description length
        if (categoryRequest.description() != null && categoryRequest.description().length() > 500) {
            throw new CategoryValidationException("Description cannot exceed 500 characters");
        }
        
        // Validate parent ID format if provided
        if (categoryRequest.parentId() != null && !categoryRequest.parentId().trim().isEmpty()) {
            String trimmedParentId = categoryRequest.parentId().trim();
            if (!trimmedParentId.matches("^[a-zA-Z0-9\\-_]+$")) {
                throw new CategoryValidationException("Parent ID must be a valid identifier");
            }
        }
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