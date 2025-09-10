package com.example.baget.buh;

public record TransactionInfoDTO(
        Long orderNo,
        Long customerId,
        Double itemsTotal,
        Double amountPaid,
        Double amountDueN,
        Double customerBalance
) {}