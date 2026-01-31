package com.example.baget.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(
            TransactionException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Transaction Error",
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        String message = "Помилка збереження даних";

        // 🔍 перевіряємо constraint
        Throwable root = ex.getRootCause();
        if (root instanceof SQLIntegrityConstraintViolationException sqlEx) {

            String sqlMessage = sqlEx.getMessage();

            if (sqlMessage != null &&
                    sqlMessage.contains("ux_invoice_per_order")) {

                message = "Одне або кілька замовлень вже включені в рахунок";
            }
        }

        Map<String, Object> body = Map.of(
                "status", HttpStatus.CONFLICT.value(),
                "error", "Business rule violation",
                "message", message,
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

}
