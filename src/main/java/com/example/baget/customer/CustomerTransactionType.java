package com.example.baget.customer;

public enum CustomerTransactionType {
    INVOICE,        // нарахування боргу
    PAYMENT,        // оплата
    ADVANCE,        // аванс (передплата)
    REFUND,         // повернення клієнту
    ADJUSTMENT      // ручна корекція
}
