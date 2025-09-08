package com.example.baget.buh;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionResource {

    private final TransactionService transactionService;

    @GetMapping("/{orderNo}")
    public List<TransactionDTO> getTransactions(@PathVariable Long orderNo) {
        return transactionService.getTransactionsByOrder(orderNo);
    }

    @GetMapping("/transaction-types")
    public List<TransactionTypeProjection> getTransactionTypes() {
        return transactionService.getAllTransactionTypes();
    }

    @GetMapping("/{orderNo}/transaction-info")
    public TransactionInfoDTO getTransactionInfo(@PathVariable Long orderNo) {
        return transactionService.getTransactionInfo(orderNo);
    }

    @PostMapping("/{orderNo}")
    public TransactionDTO createTransaction(@RequestBody TransactionDTO dto) {
        return transactionService.createTransaction(dto);
    }

    @PostMapping("/{transactionId}/complete")
    public void completeTransaction(@PathVariable Long transactionId) {
        transactionService.completeTransaction(transactionId);
    }

    @PostMapping("/{transactionId}/cancel")
    public void cancelTransaction(@PathVariable Long transactionId) {
        transactionService.cancelTransaction(transactionId);
    }



}
