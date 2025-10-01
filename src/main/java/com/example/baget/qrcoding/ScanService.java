package com.example.baget.qrcoding;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.baget.orders.OrderPrivateSummaryDTO;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.Date;

@Service

public class ScanService {
    private final OrdersRepository ordersRepository;
    private final UsersRepository userRepository;
    private final Algorithm algorithm;
    public ScanService(@Value("${QR_TOKEN}") String secret,
                       OrdersRepository ordersRepository,
                       UsersRepository userRepository) {
        this.algorithm = Algorithm.HMAC256(secret); // тут створюється Algorithm
        this.ordersRepository = ordersRepository;
        this.userRepository = userRepository;
    }

    public String createToken(Long orderId, Long branchId, long secondsValid) {
        Instant now = Instant.now();
        return JWT.create()
                .withClaim("orderId", orderId)
                .withClaim("branchId", branchId)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(secondsValid)))
                .withJWTId(java.util.UUID.randomUUID().toString())
                .sign(algorithm);
    }

    public OrderPrivateSummaryDTO scanOrder(String token, Principal principal) {
        DecodedJWT jwt = verifyToken(token);
        Long orderId = jwt.getClaim("orderId").asLong();
        Long branchId = jwt.getClaim("branchId").asLong();

        // якщо користувач не залогінений — повертаємо лише публічне summary
        if (principal == null) {
            return getPrivateSummary(orderId);
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!hasAccessToBranch(user, branchId)) {
            throw new AccessDeniedException("No rights for this branch");
        }

        return getPrivateSummary(orderId);
    }

    public void updateOrderStatus(String token, Integer newStatus, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Login required");
        }

        DecodedJWT jwt = verifyToken(token);
        Long orderId = jwt.getClaim("orderId").asLong();
        Long branchId = jwt.getClaim("branchId").asLong();

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!hasAccessToBranch(user, branchId)) {
            throw new AccessDeniedException("No rights for this branch");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        order.setStatusOrder(newStatus);
        ordersRepository.save(order);
    }

    private OrderPrivateSummaryDTO getPrivateSummary(Long orderNo) {
        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return mapToDto(order);
    }

    private OrderPrivateSummaryDTO mapToDto(Orders order) {
        return new OrderPrivateSummaryDTO(
                order.getOrderNo(),
                order.getCustomer().getCustNo(),
                order.getCustomer().getCompany(),
                order.getCustomer().getPhone(),
                order.getBranch().getName(),
                order.getSaleDate(),
                order.getShipDate(),
                order.getEmployee().getUsername(),
                order.getItemsTotal(),
                order.getStatusOrder(),
                order.getNotice()
        );
    }

    private DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    private boolean hasAccessToBranch(User user, Long branchId) {
        return user.getAllowedBranches().stream()
                .anyMatch(b -> b.getBranchNo().equals(branchId));
    }
}
