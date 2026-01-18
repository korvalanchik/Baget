package com.example.baget.branch;

import com.example.baget.customer.CustomerBalanceDTO;
import com.example.baget.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/branch", produces = MediaType.APPLICATION_JSON_VALUE)
public class BranchResource {
    private final BranchRepository branchRepository;
    private final BranchService branchService;
    private final CustomerService customerService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsBranch(@RequestParam("name") String branchName) {
        boolean exists = branchRepository.findByName(branchName).isPresent();
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/manager")
    public List<BranchDTO> getAllowedBranches(Authentication authentication) {
        return branchService.allowedBranches(authentication.getName());
    }

    @GetMapping("/clients")
    public List<CustomerBalanceDTO> getClients(
            @RequestParam(required = false) Long branchNo,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return customerService.getClientsForManager(username, branchNo);
    }

}
