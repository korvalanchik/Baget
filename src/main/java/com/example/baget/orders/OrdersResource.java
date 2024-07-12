package com.example.baget.orders;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/orderss", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrdersResource {

    private final OrdersService ordersService;

    public OrdersResource(final OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @GetMapping
    public ResponseEntity<List<OrdersDTO>> getAllOrderss() {
        return ResponseEntity.ok(ordersService.findAll());
    }

    @GetMapping("/{orderNo}")
    public ResponseEntity<OrdersDTO> getOrders(@PathVariable(name = "orderNo") final Long orderNo) {
        return ResponseEntity.ok(ordersService.get(orderNo));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createOrders(@RequestBody @Valid final OrdersDTO ordersDTO) {
        final Long createdOrderNo = ordersService.create(ordersDTO);
        return new ResponseEntity<>(createdOrderNo, HttpStatus.CREATED);
    }

    @PutMapping("/{orderNo}")
    public ResponseEntity<Long> updateOrders(@PathVariable(name = "orderNo") final Long orderNo,
            @RequestBody @Valid final OrdersDTO ordersDTO) {
        ordersService.update(orderNo, ordersDTO);
        return ResponseEntity.ok(orderNo);
    }

    @DeleteMapping("/{orderNo}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteOrders(@PathVariable(name = "orderNo") final Long orderNo) {
        ordersService.delete(orderNo);
        return ResponseEntity.noContent().build();
    }

}
