package com.example.baget.util;

import lombok.Getter;
@Getter
public class TransactionException extends RuntimeException {

    private final String code; // код помилки (наприклад: REFUND_TOO_LARGE, UNKNOWN_TYPE і т.д.)

    public TransactionException(String message) {
        super(message);
        this.code = null;
    }

    public TransactionException(String message, String code) {
        super(message);
        this.code = code;
    }
}