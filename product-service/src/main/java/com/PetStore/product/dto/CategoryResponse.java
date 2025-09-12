package com.PetStore.product.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryResponse(
    String id,
    String name,
    String description,
    String parentId,
    List<String> childIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}