package com.showtime.exception;

import com.showtime.dto.response.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            LocalDateTime.now(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        ApiResponse response = new ApiResponse(
            false,
            "Invalid email or password",
            LocalDateTime.now(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        ApiResponse response = new ApiResponse(
            false,
            "You don't have permission to access this resource",
            LocalDateTime.now(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse response = new ApiResponse(false,"Validation failed",LocalDateTime.now(),request.getDescription(false));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse> handleCustomException(
            CustomException ex, WebRequest request) {
        
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            LocalDateTime.now(),
            request.getDescription(false),
            ex.getDetails()
        );
        
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ApiResponse response = new ApiResponse(
            false,
            "An unexpected error occurred",
            LocalDateTime.now(),
            request.getDescription(false)
        );
        
        // Log the exception for debugging
        ex.printStackTrace();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}