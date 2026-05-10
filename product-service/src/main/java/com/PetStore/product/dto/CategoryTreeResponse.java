package com.PetStore.product.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryTreeResponse(
    String id,
    String name,
    String description,
    String parentId,
    List<CategoryTreeResponse> children,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}