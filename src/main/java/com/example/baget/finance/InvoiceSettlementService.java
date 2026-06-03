package com.example.baget.finance;

import com.example.baget.customer.AdvanceAllocationService;
import com.example.baget.customer.CustomerTransactionDTO;
import com.example.baget.invoices.InvoicePaymentRequest;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceSettlementService {
    private final AdvanceAllocationService advanceAllocationService;
    private final PaymentOrchestrator paymentOrchestrator;
    private final InvoiceOperationContextService invoiceOperationContextService;
    private final UsersRepository usersRepository;

    @Transactional
    public List<CustomerTransactionDTO> settleInvoice(
            InvoicePaymentRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        InvoiceOperationContext ctx = invoiceOperationContextService.resolveContext(request, user);

        List<CustomerTransactionDTO> result = new ArrayList<>();

        BigDecimal allocationAmount = Optional.ofNullable(request.allocationAmount()).orElse(BigDecimal.ZERO);

        if (allocationAmount.compareTo(BigDecimal.ZERO) > 0) {
            result.add(advanceAllocationService.allocateAdvance(ctx, allocationAmount, request.note()));
        }

        BigDecimal paymentAmount = Optional.ofNullable(request.amount()).orElse(BigDecimal.ZERO);

        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            result.addAll(paymentOrchestrator.processPayment(ctx, request, user));
        }

        return result;
    }

    @Transactional
    public List<CustomerTransactionDTO> settleAdvance(
            InvoicePaymentRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        InvoiceOperationContext ctx = invoiceOperationContextService.resolveContext(request, user);

        return new ArrayList<>(paymentOrchestrator.processPayment(ctx, request, user));
    }

}
