# Design Document

## Overview

The product classification feature extends the existing product service with hierarchical category management capabilities. The design follows the established patterns in the current codebase, using Spring Boot with MongoDB for persistence, and maintaining the existing layered architecture (Controller → Service → Repository → Model).

The classification system introduces two new core entities: `Category` and the relationship between `Product` and `Category`. Categories support hierarchical structures with parent-child relationships, and products can be assigned to multiple categories.

## Architecture

### High-Level Architecture

The classification feature integrates seamlessly with the existing product service architecture:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │    │    Services     │    │  Repositories   │
│                 │    │                 │    │                 │
│ ProductController│◄──►│ ProductService  │◄──►│ProductRepository│
│ CategoryController│   │ CategoryService │    │CategoryRepository│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Data Models   │
                       │                 │
                       │    Product      │
                       │    Category     │
                       └─────────────────┘
```

### Data Flow

1. **Category Management**: REST API requests → CategoryController → CategoryService → CategoryRepository → MongoDB
2. **Product Classification**: Enhanced ProductService handles category assignments during product operations
3. **Category Queries**: Support for filtering products by category through enhanced ProductService methods

## Components and Interfaces

### Core Models

#### Category Model
```java
@Document(value = "category")
public class Category {
    private String id;
    private String name;
    private String description;
    private String parentId;  // Reference to parent category
    private List<String> childIds; // References to child categories
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Enhanced Product Model
```java
@Document(value = "product")
public class Product {
    // Existing fields...
    private List<String> categoryIds; // References to assigned categories
}
```

### DTOs and Request/Response Objects

#### Category DTOs
- `CategoryRequest`: For creating/updating categories
- `CategoryResponse`: For API responses
- `CategoryTreeResponse`: For hierarchical category display

#### Enhanced Product DTOs
- Enhanced `ProductRequest`: Include optional category assignments
- Enhanced `ProductResponse`: Include assigned category information

### Service Layer

#### CategoryService
- `createCategory(CategoryRequest)`: Create new categories with validation
- `updateCategory(String id, CategoryRequest)`: Update existing categories
- `deleteCategory(String id)`: Delete categories with constraint checking
- `getCategoryById(String id)`: Retrieve single category
- `getAllCategories()`: Retrieve all categories
- `getCategoryTree()`: Build hierarchical category structure
- `getChildCategories(String parentId)`: Get direct children of a category

#### Enhanced ProductService
- Enhanced `createProduct()`: Support category assignment during creation
- Enhanced `updateProduct()`: Support category modification
- `getProductsByCategory(String categoryId)`: Filter products by category
- `getProductsByCategoryHierarchy(String categoryId)`: Include subcategory products

### Repository Layer

#### CategoryRepository
```java
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);
    List<Category> findByParentIdIsNull(); // Root categories
    boolean existsByParentId(String parentId);
}
```

#### Enhanced ProductRepository
```java
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoryIdsContaining(String categoryId);
    List<Product> findByCategoryIdsIn(List<String> categoryIds);
}
```

### Controller Layer

#### CategoryController
- `POST /api/category`: Create category
- `GET /api/category`: Get all categories
- `GET /api/category/{id}`: Get category by ID
- `GET /api/category/tree`: Get category hierarchy
- `PUT /api/category/{id}`: Update category
- `DELETE /api/category/{id}`: Delete category
- `GET /api/category/{id}/children`: Get child categories

#### Enhanced ProductController
- Enhanced existing endpoints to support category filtering
- `GET /api/product?categoryId={id}`: Filter products by category
- `GET /api/product?categoryId={id}&includeSubcategories=true`: Include subcategory products

## Data Models

### Category Document Structure
```json
{
  "_id": "category_id",
  "name": "Cats",
  "description": "Products for cats and kittens",
  "parentId": null,
  "childIds": ["cat_food_id", "cat_toys_id"],
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Enhanced Product Document Structure
```json
{
  "_id": "product_id",
  "name": "Premium Cat Food",
  "description": "High-quality dry food for adult cats",
  "skuCode": "CF-001",
  "price": 24.99,
  "categoryIds": ["cats_id", "cat_food_id"]
}
```

### Category Hierarchy Example
```
Cats (root)
├── Cat Food
│   ├── Dry Food
│   └── Wet Food
└── Cat Toys
    ├── Interactive Toys
    └── Catnip Toys

Dogs (root)
├── Dog Food
│   ├── Puppy Food
│   └── Adult Food
└── Dog Toys
    ├── Chew Toys
    └── Fetch Toys
```

## Error Handling

### Validation Rules
- Category names must be unique within the same parent level
- Circular references in category hierarchy are prevented
- Products cannot be assigned to non-existent categories
- Categories with assigned products cannot be deleted

### Error Response Format
Following the existing pattern, errors return appropriate HTTP status codes with descriptive messages:

```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Category name already exists at this level",
  "path": "/api/category"
}
```

### Exception Handling
- `CategoryNotFoundException`: When category ID doesn't exist
- `CategoryHierarchyException`: For circular reference attempts
- `CategoryDeletionException`: When trying to delete categories with products
- `CategoryValidationException`: For validation failures

## Testing Strategy

### Unit Testing
- **Model Tests**: Validate entity relationships and constraints
- **Service Tests**: Test business logic with mocked repositories
- **Repository Tests**: Test MongoDB queries and data access
- **Controller Tests**: Test REST endpoints with MockMvc

### Integration Testing
- **Database Integration**: Test actual MongoDB operations
- **API Integration**: End-to-end REST API testing
- **Category Hierarchy**: Test complex parent-child relationships

### Test Data Strategy
- Use embedded MongoDB for integration tests
- Create test fixtures for category hierarchies
- Mock external dependencies in unit tests

### Key Test Scenarios
1. **Category CRUD Operations**: Create, read, update, delete categories
2. **Hierarchy Management**: Parent-child relationships, circular reference prevention
3. **Product-Category Association**: Assignment, removal, querying
4. **Constraint Validation**: Deletion prevention, uniqueness checks
5. **API Error Handling**: Invalid requests, not found scenarios
6. **Performance**: Large category trees, bulk product queries

### Testing Tools
- JUnit 5 for unit testing framework
- Mockito for mocking dependencies
- TestContainers for MongoDB integration testing
- Spring Boot Test for web layer testing
- AssertJ for fluent assertions