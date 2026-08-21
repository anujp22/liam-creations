package com.codewithanuj.catalog.shared.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.valueOf(exception.getStatusCode().value()),
                exception.getReason(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for '" + exception.getName() + "': " + exception.getValue();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    /**
     * An unparseable request body — most often a value that is not one of the accepted
     * enum constants, which Jackson rejects before the controller ever runs.
     *
     * <p>Without this the request is resolved by Spring's default handler, which sets
     * the status and re-dispatches to {@code /error}. That path produces a body with no
     * message at all ({@code server.error.include-message} is {@code never}), leaving an
     * admin staring at a bare 400 with nothing pointing at the request body. It cost
     * real debugging time twice, so the message names the field and the accepted values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, describe(exception), request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String supported = exception.getSupportedHttpMethods() == null
                ? "none"
                : exception.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.joining(", "));

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                exception.getMethod() + " is not supported here. Supported: " + supported,
                request.getRequestURI()
        );
    }

    /**
     * A URL that matches no handler and no static resource — a missing upload, most
     * likely. Handled here so it comes back as the same JSON shape as every other
     * error rather than Spring's default error page.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not found.", request.getRequestURI());
    }

    /**
     * Turns Jackson's internal complaint into one line an admin can act on, without
     * echoing the raw parser message (which carries class names and stack context).
     */
    private String describe(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof InvalidFormatException invalid) {
            String field = invalid.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            String target = invalid.getTargetType() != null && invalid.getTargetType().isEnum()
                    ? " Accepted values: " + Arrays.toString(invalid.getTargetType().getEnumConstants()) + "."
                    : "";

            return "Invalid value for '" + (field.isBlank() ? "request body" : field) + "': "
                    + invalid.getValue() + "." + target;
        }
        return "Request body could not be read. Check that it is valid JSON and that every "
                + "field has the expected type.";
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(status).body(body);
    }
}
