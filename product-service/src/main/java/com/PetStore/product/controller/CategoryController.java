package com.PetStore.product.controller;

import com.PetStore.product.dto.CategoryRequest;
import com.PetStore.product.dto.CategoryResponse;
import com.PetStore.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return categoryService.createCategory(categoryRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponse getCategoryById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping("/tree")
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryResponse> getCategoryTree() {
        return categoryService.getCategoryTree();
    }

    @GetMapping("/{id}/children")
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryResponse> getChildCategories(@PathVariable String id) {
        return categoryService.getChildCategories(id);
    }

    @GetMapping("/root")
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryResponse> getRootCategories() {
        return categoryService.getRootCategories();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponse updateCategory(@PathVariable String id, 
                                         @Valid @RequestBody CategoryRequest categoryRequest) {
        return categoryService.updateCategory(id, categoryRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
    }
}