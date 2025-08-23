package com.example.baget.buh;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private Long transactionId;
    private Long orderNo;

    private Long transactionTypeId;   // typeId
    private String transactionTypeCode;
    private String transactionTypeDescription;

    private OffsetDateTime transactionDate;
    private Double amount;
    private String reference;
    private String status;
    private String note;

}