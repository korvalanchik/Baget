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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("api/qrcode/invoices")
public class QrResource {

    private final ScanService scanService;
    private final QrGenerator qrGenerator;
    private final OrdersRepository ordersRepository;

    @Value("${api.server.url}")
    private String serverUrl;

    @GetMapping("/{id}/qr.png")
    public ResponseEntity<byte[]> getInvoiceQr(@PathVariable Long id) throws Exception {
        Optional<Orders> inv = Optional.ofNullable(ordersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id)));
        String url = serverUrl + "/api/qrcode/invoices/public/" + inv.get().getPublicId();

        byte[] png = qrGenerator.generateQrPng(url, 300, 300);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }


    @GetMapping("/public/{publicId}")
    public Object getPublicOrder(
            @PathVariable String publicId,
            Principal principal,
            Model model,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {
        boolean wantsJson = acceptHeader == null || acceptHeader.contains("application/json");
        Object result = scanService.scanOrder(publicId, principal);

        // 🔹 Якщо клієнт просить JSON (API-запит або SPA)
        if (wantsJson) {
            return ResponseEntity.ok(result);
        }

        // 🔹 Якщо клієнт звичайний браузер → повертаємо HTML через Thymeleaf
        model.addAttribute("order", result);
        return "orders/public-order";
    }

    @PutMapping("/scan/{publicId}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable String publicId,
                                                  @RequestParam Integer status,
                                                  Principal principal) {
        scanService.updateOrderStatus(publicId, status, principal);
        return ResponseEntity.noContent().build();
    }

}