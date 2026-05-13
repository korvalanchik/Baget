package com.example.baget.ledger;

import org.springframework.stereotype.Service;

@Service
public class LedgerService {
    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public LedgerEntry createEntry(LedgerRequest request) {

        return ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(request.branch())
                        .direction(request.direction())
                        .category(request.category())
                        .amount(request.amount())
                        .createdAt(request.createdAt())
                        .createdBy(request.createdBy())

                        .customerId(request.customerId())
                        .payer(request.payer())
                        .invoiceId(request.invoiceId())
                        .reference(request.reference())
                        .note(request.note())
                        .build()
        );
    }
}