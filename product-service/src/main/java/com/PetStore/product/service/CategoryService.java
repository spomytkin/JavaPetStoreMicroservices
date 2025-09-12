package com.PetStore.product.service;

import com.PetStore.product.model.Category;
import com.PetStore.product.repository.CategoryRepository;
import com.PetStore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public Category createCategory(String name, String description, String parentId) {
        log.info("Creating category with name: {}, parentId: {}", name, parentId);
        
        // Validate category name uniqueness within the same parent level
        validateCategoryNameUniqueness(name, parentId);
        
        // Validate parent exists if parentId is provided
        if (parentId != null) {
            validateParentExists(parentId);
        }
        
        LocalDateTime now = LocalDateTime.now();
        Category category = Category.builder()
                .name(name)
                .description(description)
                .parentId(parentId)
                .childIds(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        
        // Update parent's childIds if this is a subcategory
        if (parentId != null) {
            updateParentChildIds(parentId, savedCategory.getId());
        }
        
        log.info("Category created successfully with id: {}", savedCategory.getId());
        return savedCategory;
    }

    public Category updateCategory(String id, String name, String description) {
        log.info("Updating category with id: {}", id);
        
        Category category = getCategoryById(id);
        
        // Validate name uniqueness if name is being changed
        if (!category.getName().equals(name)) {
            validateCategoryNameUniqueness(name, category.getParentId());
        }
        
        category.setName(name);
        category.setDescription(description);
        category.setUpdatedAt(LocalDateTime.now());
        
        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", id);
        return updatedCategory;
    }

    public void deleteCategory(String id) {
        log.info("Deleting category with id: {}", id);
        
        Category category = getCategoryById(id);
        
        // Check if category has products assigned
        if (hasProductsAssigned(id)) {
            throw new IllegalStateException("Cannot delete category with assigned products");
        }
        
        // Check if category has child categories
        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalStateException("Cannot delete category with child categories");
        }
        
        // Remove this category from parent's childIds
        if (category.getParentId() != null) {
            removeFromParentChildIds(category.getParentId(), id);
        }
        
        categoryRepository.deleteById(id);
        log.info("Category deleted successfully with id: {}", id);
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIdIsNull();
    }

    public List<Category> getChildCategories(String parentId) {
        validateParentExists(parentId);
        return categoryRepository.findByParentId(parentId);
    }

    public List<Category> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
        
        return categoryMap.values().stream()
                .filter(category -> category.getParentId() == null)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategoryIdsInHierarchy(String categoryId) {
        Set<String> categoryIds = new HashSet<>();
        collectCategoryIdsRecursively(categoryId, categoryIds);
        return new ArrayList<>(categoryIds);
    }

    private void validateCategoryNameUniqueness(String name, String parentId) {
        boolean exists;
        if (parentId == null) {
            exists = categoryRepository.existsByNameAndParentIdIsNull(name);
        } else {
            exists = categoryRepository.existsByNameAndParentId(name, parentId);
        }
        
        if (exists) {
            throw new IllegalArgumentException("Category name already exists at this level");
        }
    }

    private void validateParentExists(String parentId) {
        if (!categoryRepository.existsById(parentId)) {
            throw new IllegalArgumentException("Parent category not found with id: " + parentId);
        }
    }

    private void validateNoCircularReference(String categoryId, String newParentId) {
        if (categoryId.equals(newParentId)) {
            throw new IllegalArgumentException("Category cannot be its own parent");
        }
        
        Set<String> visited = new HashSet<>();
        String currentParentId = newParentId;
        
        while (currentParentId != null && !visited.contains(currentParentId)) {
            if (currentParentId.equals(categoryId)) {
                throw new IllegalArgumentException("Circular reference detected in category hierarchy");
            }
            
            visited.add(currentParentId);
            Category parent = categoryRepository.findById(currentParentId).orElse(null);
            currentParentId = parent != null ? parent.getParentId() : null;
        }
    }

    private boolean hasProductsAssigned(String categoryId) {
        // This will be implemented when Product model is enhanced with categoryIds
        // For now, return false as products don't have category associations yet
        return false;
    }

    private void updateParentChildIds(String parentId, String childId) {
        Category parent = getCategoryById(parentId);
        if (!parent.getChildIds().contains(childId)) {
            parent.getChildIds().add(childId);
            parent.setUpdatedAt(LocalDateTime.now());
            categoryRepository.save(parent);
        }
    }

    private void removeFromParentChildIds(String parentId, String childId) {
        Category parent = getCategoryById(parentId);
        parent.getChildIds().remove(childId);
        parent.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(parent);
    }

    private void collectCategoryIdsRecursively(String categoryId, Set<String> categoryIds) {
        categoryIds.add(categoryId);
        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            collectCategoryIdsRecursively(child.getId(), categoryIds);
        }
    }
}