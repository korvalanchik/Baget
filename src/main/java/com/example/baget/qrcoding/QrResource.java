package com.example.baget.qrcoding;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
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
        // приклад: токен дійсний 24 години (86400 sec)
        String token = scanService.createToken(id, inv.get().getBranch().getBranchNo(), 86400);
        String url = serverUrl + "/qrcode/invoices/scan?token=" + token;
        byte[] png = qrGenerator.generateQrPng(url, 400, 400);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }

    @GetMapping("/scan")
    public ResponseEntity<?> scan(@RequestParam String token, Principal principal) {
        try {
            return ResponseEntity.ok(scanService.scanOrder(token, principal));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }
    }

    @PostMapping("/scan/action")
    public ResponseEntity<?> changeStatus(@RequestParam String token,
                                          @RequestParam Integer status,
                                          Principal principal) {
        try {
            scanService.updateOrderStatus(token, status, principal);
            return ResponseEntity.ok("Status updated");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }
    }

}