package com.example.baget.customer;

public enum CustomerTransactionType {
    INVOICE,         // нарахування боргу
    PAYMENT,         // оплата
    ALLOCATION,      // рознесення оплати по замовленнях
    ADVANCE,         // аванс (передплата)
    ADVANCE_APPLIED, // застосування авансу
    REFUND,          // повернення клієнту
    ADJUSTMENT       // ручна корекція
}
