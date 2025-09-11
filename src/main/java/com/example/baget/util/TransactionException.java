package com.example.baget.util;

import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
public class TransactionException extends RuntimeException {
    private final String code;         // код помилки (опційно)
    private final HttpStatus status;   // HTTP статус (можна завжди BAD_REQUEST)

    public TransactionException(String message) {
        this(message, null, HttpStatus.BAD_REQUEST);
    }

    public TransactionException(String message, String code) {
        this(message, code, HttpStatus.BAD_REQUEST);
    }

    public TransactionException(String message, String code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
