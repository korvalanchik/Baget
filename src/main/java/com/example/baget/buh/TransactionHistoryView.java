package com.example.baget.buh;

import java.time.OffsetDateTime;

public interface TransactionHistoryView {
    Long getTransactionId();
    OffsetDateTime getTransactionDate();
    TransactionType getTransactionType();
    Double getAmount();
    String getReference();
    String getNote();
}

