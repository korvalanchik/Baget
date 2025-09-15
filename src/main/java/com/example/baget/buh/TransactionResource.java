package com.example.baget.buh;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/collective-invoice")
    public ResponseEntity<Long> createCollectiveInvoice(@RequestBody List<Long> orderNos) {
        return ResponseEntity.ok(transactionService.createCollectiveInvoice(orderNos));
    }

    @PostMapping("/payments")
    public List<TransactionDTO> createBatchPayments(@RequestBody List<Long> orderNos) {
        return transactionService.createBatchPayments(orderNos);
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
