package com.example.baget.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(
            TransactionException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = Map.of(
                "status", ex.getStatus().value(),
                "error", "Transaction Error",
                "code", ex.getCode(),            // опціонально
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

}
