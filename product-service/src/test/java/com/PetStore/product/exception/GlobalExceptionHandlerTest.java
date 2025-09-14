package com.PetStore.product.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/category");
        webRequest = new ServletWebRequest(request);
    }

    @Test
    void handleCategoryNotFoundException_ShouldReturnNotFound() {
        // Given
        CategoryNotFoundException exception = new CategoryNotFoundException("Category not found with id: test-id");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleCategoryNotFoundException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Category not found with id: test-id", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleCategoryValidationException_ShouldReturnBadRequest() {
        // Given
        CategoryValidationException exception = new CategoryValidationException("Category name is required");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleCategoryValidationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Category name is required", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleCategoryHierarchyException_ShouldReturnBadRequest() {
        // Given
        CategoryHierarchyException exception = new CategoryHierarchyException("Circular reference detected");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleCategoryHierarchyException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Circular reference detected", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleCategoryDeletionException_ShouldReturnConflict() {
        // Given
        CategoryDeletionException exception = new CategoryDeletionException("Cannot delete category with assigned products");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleCategoryDeletionException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Cannot delete category with assigned products", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("categoryRequest", "name", "Category name is required");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response = 
            globalExceptionHandler.handleValidationExceptions(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Validation Failed", response.getBody().error());
        assertEquals("Input validation failed", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
        assertTrue(response.getBody().validationErrors().containsKey("name"));
        assertEquals("Category name is required", response.getBody().validationErrors().get("name"));
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Invalid argument provided", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleDataAccessException_ShouldReturnInternalServerError() {
        // Given
        DataAccessException exception = new DataAccessException("Database connection failed") {};

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleDataAccessException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("A database error occurred while processing your request", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleDuplicateKeyException_ShouldReturnConflict() {
        // Given
        DuplicateKeyException exception = new DuplicateKeyException("Duplicate key error");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleDuplicateKeyException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("A resource with the same identifier already exists", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleGenericException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertEquals("/api/category", response.getBody().path());
    }
}