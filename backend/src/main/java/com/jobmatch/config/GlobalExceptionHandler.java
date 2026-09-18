package com.jobmatch.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

/**
 * Handles exceptions directly (writing the response in-place) instead of letting them fall through
 * to Servlet error-page dispatch. A sendError()-triggered forward to /error re-runs the whole security
 * filter chain on a fresh dispatch, which gets rejected by authorizeHttpRequests and masks every
 * non-2xx status (404, 400, 502, ...) as an empty 403 — this sidesteps that entirely.
 *
 * Covers two distinct sources of non-2xx responses:
 *  - ResponseStatusException: thrown explicitly by our own controllers/services.
 *  - NoHandlerFoundException: a genuinely unmapped URL (no controller method matches at all) — this
 *    goes through DispatcherServlet's default sendError() path rather than a thrown exception unless
 *    spring.mvc.throw-exception-if-no-handler-found is enabled (see application.yml), which is what
 *    lets it land here too instead of hitting the same forward-based bug.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "status", ex.getStatusCode().value(),
                        "message", ex.getReason() != null ? ex.getReason() : ex.getMessage()
                ));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "message", "No endpoint " + ex.getHttpMethod() + " " + ex.getRequestURL()
                ));
    }
}
