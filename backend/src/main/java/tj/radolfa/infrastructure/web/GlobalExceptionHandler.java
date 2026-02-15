package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 *
 * <p>Provides consistent error responses across all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Spring Security access denied exceptions.
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponseDto> handleAccessDenied(AccessDeniedException ex) {
        LOG.warn("[SECURITY] Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponseDto.error("Access denied. Insufficient permissions."));
    }

    /**
     * Handles Spring Security authentication exceptions.
     * Returns 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponseDto> handleAuthenticationException(AuthenticationException ex) {
        LOG.warn("[SECURITY] Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponseDto.error("Authentication required."));
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Returns 400 Bad Request with field-level errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);

        LOG.debug("[VALIDATION] Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles illegal argument exceptions.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("[VALIDATION] Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(MessageResponseDto.error(ex.getMessage()));
    }

    /**
     * Handles unique constraint violations (e.g. duplicate email).
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOG.warn("[CONSTRAINT] Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        String message = "A conflict occurred with existing data.";
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains("email")) {
            message = "This email address is already in use.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponseDto.error(message));
    }

    /**
     * Handles all other unhandled exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponseDto> handleGenericException(Exception ex) {
        LOG.error("[ERROR] Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponseDto.error("An unexpected error occurred. Please try again later."));
    }
}
