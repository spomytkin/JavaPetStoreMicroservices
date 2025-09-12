package com.PetStore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,
    
    String parentId
) {
}