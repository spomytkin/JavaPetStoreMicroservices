package com.PetStore.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(String id, String name, String description,
                             String skuCode, BigDecimal price, List<String> categoryIds) { }
