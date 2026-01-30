package com.example.baget.invoices;

import com.example.baget.customer.CustomerIssueInvoiceRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceResource {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(
            @RequestBody CustomerIssueInvoiceRequestDTO request
    ) {
        return ResponseEntity.ok(invoiceService.createInvoiceForOrders(request));
    }
}
