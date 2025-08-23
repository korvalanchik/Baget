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

    @PostMapping("/{orderNo}")
    public TransactionDTO addTransaction(@PathVariable Long orderNo,
                                         @RequestBody TransactionDTO transactionDTO) {
        return transactionService.addTransaction(orderNo, transactionDTO);
    }

    @GetMapping("/transaction-types")
    public List<TransactionTypeDTO> getTransactionTypes() {
        return transactionService.getAllTransactionTypes();
    }

}
