package com.example.baget.customer;

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
@RequestMapping(value = "/api/customers", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerResource {

    private final CustomerService customerService;

    public CustomerResource(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{custNo}")
    public ResponseEntity<CustomerDTO> getCustomer(
            @PathVariable(name = "custNo") final Long custNo) {
        return ResponseEntity.ok(customerService.get(custNo));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createCustomer(@RequestBody @Valid final CustomerDTO customerDTO) {
        final Long createdCustNo = customerService.create(customerDTO);
        return new ResponseEntity<>(createdCustNo, HttpStatus.CREATED);
    }

    @PutMapping("/{custNo}")
    public ResponseEntity<Long> updateCustomer(@PathVariable(name = "custNo") final Long custNo,
            @RequestBody @Valid final CustomerDTO customerDTO) {
        customerService.update(custNo, customerDTO);
        return ResponseEntity.ok(custNo);
    }

    @DeleteMapping("/{custNo}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteCustomer(@PathVariable(name = "custNo") final Long custNo) {
        customerService.delete(custNo);
        return ResponseEntity.noContent().build();
    }

}
