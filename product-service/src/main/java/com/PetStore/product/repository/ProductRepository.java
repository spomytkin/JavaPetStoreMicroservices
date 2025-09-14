package com.PetStore.product.repository;

import com.PetStore.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    
    /**
     * Find products that contain the specified category ID in their categoryIds list
     * @param categoryId the category ID to search for
     * @return list of products that belong to the specified category
     */
    List<Product> findByCategoryIdsContaining(String categoryId);
    
    /**
     * Find products that have any of the specified category IDs in their categoryIds list
     * @param categoryIds list of category IDs to search for
     * @return list of products that belong to any of the specified categories
     */
    List<Product> findByCategoryIdsIn(List<String> categoryIds);
}
