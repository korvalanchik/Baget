package com.example.baget.invoices;

import com.example.baget.customer.CustomerIssueInvoiceRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceResource {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(
            @RequestBody CustomerIssueInvoiceRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(invoiceService.createInvoiceForOrders(request, authentication));
    }

    @GetMapping("/{id}")
    public InvoiceDetailsDTO getInvoice(@PathVariable Long id) {
        return invoiceService.getInvoice(id);
    }

}
