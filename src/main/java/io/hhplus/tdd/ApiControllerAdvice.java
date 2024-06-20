package io.hhplus.tdd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.hhplus.tdd.point.PointController;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.status(500).body(new ErrorResponse("500", e.getMessage()));
    }
}
