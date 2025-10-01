package com.example.baget.qrcoding;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/invoices")
public class InvoiceQrController {
    private final ScanService tokenService;
    private final QrGenerator qrGenerator;
    private final OrdersRepository ordersRepository;

    @Value("${api.server.url}")
    private String serverUrl;

    @GetMapping("/{id}/qr.png")
    public ResponseEntity<byte[]> getInvoiceQr(@PathVariable Long id) throws Exception {
        Optional<Orders> inv = Optional.ofNullable(ordersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id)));
        // приклад: токен дійсний 24 години (86400 sec)
        String token = tokenService.createToken(id, inv.get().getBranch().getBranchNo(), 86400);
        String url = serverUrl + "/scan?token=" + token;
        byte[] png = qrGenerator.generateQrPng(url, 400, 400);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }
}