package com.PetStore.product.controller;

import com.PetStore.product.dto.ProductRequest;
import com.PetStore.product.dto.ProductResponse;
import com.PetStore.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody ProductRequest productRequest) {
        return productService.createProduct(productRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponse> getAllProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean includeSubcategories) {
        
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            if (includeSubcategories) {
                return productService.getProductsByCategoryHierarchy(categoryId);
            } else {
                return productService.getProductsByCategory(categoryId);
            }
        }
        
        return productService.getAllProducts();
    }
}
