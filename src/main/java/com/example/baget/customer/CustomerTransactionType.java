package com.example.baget.customer;

public enum CustomerTransactionType {
    INVOICE,         // нарахування боргу
    PAYMENT,         // оплата
    ADVANCE,         // аванс (передплата)
    ADVANCE_APPLIED, // застосування авансу
    REFUND,          // повернення клієнту
    ADJUSTMENT       // ручна корекція
}
