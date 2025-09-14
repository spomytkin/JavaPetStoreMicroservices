package com.PetStore.product.repository;

import com.PetStore.product.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);
    List<Category> findByParentIdIsNull();
    boolean existsByParentId(String parentId);
    List<Category> findByNameAndParentId(String name, String parentId);
    List<Category> findByNameAndParentIdIsNull(String name);
    boolean existsByNameAndParentId(String name, String parentId);
    boolean existsByNameAndParentIdIsNull(String name);
}