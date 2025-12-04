package compass.career.evaluationsapi.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    // ============= EXCEPCIONES DE VALIDACIÓN =============

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = buildErrorResponse(
                "VALIDATION_ERROR",
                "Validation error in the data sent",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
        body.put("fields", fieldErrors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = buildErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = buildErrorResponse(
                "INVALID_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ============= EXCEPCIONES DE BASE DE DATOS =============

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, Object> body = buildErrorResponse(
                "NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Data integrity error";
        String code = "DATA_INTEGRITY_ERROR";

        String exMessage = ex.getMessage().toLowerCase();

        if (exMessage.contains("duplicate") || exMessage.contains("unique")) {
            message = "A record with this data already exists";
            code = "DUPLICATE_ENTRY";
        } else if (exMessage.contains("foreign key") || exMessage.contains("referential integrity")) {
            message = "Reference to non-existent data. Verifies that the related data exists.";
            code = "FOREIGN_KEY_VIOLATION";
        } else if (exMessage.contains("null") || exMessage.contains("not-null")) {
            message = "Mandatory data is missing from the application";
            code = "NULL_VALUE_ERROR";
        }

        Map<String, Object> body = buildErrorResponse(code, message, HttpStatus.CONFLICT);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, Object>> handleSQLException(SQLException ex) {
        String message = "Error connecting to the database";
        String code = "DATABASE_CONNECTION_ERROR";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

        String exMessage = ex.getMessage().toLowerCase();

        if (exMessage.contains("authentication failed") ||
                exMessage.contains("password authentication failed") ||
                exMessage.contains("access denied") ||
                exMessage.contains("invalid authorization")) {
            message = "Database authentication failed. Please check database credentials";
            code = "DATABASE_AUTH_FAILED";
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (exMessage.contains("database") && exMessage.contains("does not exist")) {
            message = "The specified database does not exist";
            code = "DATABASE_NOT_FOUND";
        } else if (exMessage.contains("timeout") || exMessage.contains("timed out")) {
            message = "Database connection timeout";
            code = "DATABASE_TIMEOUT";
        }

        Map<String, Object> body = buildErrorResponse(code, message, status);

        if (isDevelopmentMode()) {
            body.put("sqlState", ex.getSQLState());
            body.put("errorCode", ex.getErrorCode());
            body.put("details", ex.getMessage());
        }

        return ResponseEntity.status(status).body(body);
    }

    // ============= EXCEPCIONES DE APIS EXTERNAS =============

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientError(HttpClientErrorException ex) {
        String message = "Error calling external API";
        String code = "EXTERNAL_API_CLIENT_ERROR";
        HttpStatus status = HttpStatus.BAD_GATEWAY;

        int statusCode = ex.getStatusCode().value();

        if (statusCode == 401) {
            message = "External API authentication failed. Please check API key";
            code = "EXTERNAL_API_UNAUTHORIZED";
        } else if (statusCode == 403) {
            message = "Access forbidden to external API";
            code = "EXTERNAL_API_FORBIDDEN";
        } else if (statusCode == 404) {
            message = "External API endpoint not found";
            code = "EXTERNAL_API_NOT_FOUND";
        } else if (statusCode == 429) {
            message = "External API rate limit exceeded";
            code = "EXTERNAL_API_RATE_LIMIT";
            status = HttpStatus.TOO_MANY_REQUESTS;
        }

        Map<String, Object> body = buildErrorResponse(code, message, status);
        body.put("externalApiStatus", statusCode);

        if (isDevelopmentMode()) {
            body.put("externalApiResponse", ex.getResponseBodyAsString());
        }

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException ex) {
        String message = "External API server error";
        String code = "EXTERNAL_API_SERVER_ERROR";

        Map<String, Object> body = buildErrorResponse(code, message, HttpStatus.BAD_GATEWAY);
        body.put("externalApiStatus", ex.getStatusCode().value());

        if (isDevelopmentMode()) {
            body.put("externalApiResponse", ex.getResponseBodyAsString());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        String message = "Cannot connect to external service";
        String code = "EXTERNAL_SERVICE_UNAVAILABLE";

        if (ex.getCause() instanceof SocketTimeoutException) {
            message = "External service timeout";
            code = "EXTERNAL_SERVICE_TIMEOUT";
        } else if (ex.getMessage().contains("Connection refused")) {
            message = "External service connection refused";
            code = "EXTERNAL_SERVICE_CONNECTION_REFUSED";
        } else if (ex.getMessage().contains("Connection timed out")) {
            message = "External service connection timed out";
            code = "EXTERNAL_SERVICE_CONNECTION_TIMEOUT";
        }

        Map<String, Object> body = buildErrorResponse(code, message, HttpStatus.SERVICE_UNAVAILABLE);

        if (isDevelopmentMode()) {
            body.put("details", ex.getMessage());
            body.put("cause", ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "Unknown");
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(SocketTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleSocketTimeout(SocketTimeoutException ex) {
        String message = "Request timeout. The operation took too long to complete";
        String code = "REQUEST_TIMEOUT";

        Map<String, Object> body = buildErrorResponse(code, message, HttpStatus.GATEWAY_TIMEOUT);

        if (isDevelopmentMode()) {
            body.put("details", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    // ============= EXCEPCIONES DE HTTP =============

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format(
                "The HTTP method '%s' is not allowed for this route. Allowed methods: %s",
                ex.getMethod(),
                String.join(", ", ex.getSupportedMethods())
        );

        Map<String, Object> body = buildErrorResponse(
                "METHOD_NOT_ALLOWED",
                message,
                HttpStatus.METHOD_NOT_ALLOWED
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        Map<String, Object> body = buildErrorResponse(
                "RESOURCE_NOT_FOUND",
                "The requested route does not exist on the server",
                HttpStatus.NOT_FOUND
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ============= EXCEPCIONES DE PARÁMETROS =============

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = String.format(
                "The required parameter '%s' of type %s is missing",
                ex.getParameterName(),
                ex.getParameterType()
        );

        Map<String, Object> body = buildErrorResponse(
                "MISSING_PARAMETER",
                message,
                HttpStatus.BAD_REQUEST
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "The parameter '%s' has an invalid value. The type %s was expected",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        Map<String, Object> body = buildErrorResponse(
                "INVALID_PARAMETER_TYPE",
                message,
                HttpStatus.BAD_REQUEST
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ============= EXCEPCIONES DE PARSEO JSON =============

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "The JSON format is invalid";
        String code = "INVALID_JSON";

        String exMessage = ex.getMessage();

        if (exMessage.contains("LocalDate")) {
            message = "The date format must be YYYY-MM-DD (example: 2000-01-15)";
            code = "INVALID_DATE_FORMAT";
        } else if (exMessage.contains("LocalDateTime")) {
            message = "The date and time format must be YYYY-MM-DDTHH:mm:ss";
            code = "INVALID_DATETIME_FORMAT";
        } else if (exMessage.contains("JSON parse error")) {
            message = "The submitted JSON contains syntax errors";
            code = "JSON_SYNTAX_ERROR";
        } else if (exMessage.contains("Required request body is missing")) {
            message = "A JSON body is required in the request";
            code = "MISSING_REQUEST_BODY";
        } else if (exMessage.contains("Cannot deserialize")) {
            message = "One or more fields have the wrong data type";
            code = "TYPE_MISMATCH";
        }

        Map<String, Object> body = buildErrorResponse(code, message, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ============= EXCEPCIÓN GENÉRICA =============

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        ex.printStackTrace();

        Map<String, Object> body = buildErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later",
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        if (isDevelopmentMode()) {
            body.put("details", ex.getMessage());
            body.put("exceptionType", ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ============= MÉTODOS AUXILIARES =============

    private Map<String, Object> buildErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    private boolean isDevelopmentMode() {
        String env = System.getProperty("spring.profiles.active");
        return env == null || env.equals("dev") || env.equals("development");
    }
}