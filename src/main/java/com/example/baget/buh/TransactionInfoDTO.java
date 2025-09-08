package com.example.baget.buh;

public record TransactionInfoDTO(
        Long orderNo,
        Double itemsTotal,
        Double amountPaid,
        Double amountDueN,
        Double customerBalance
) {}