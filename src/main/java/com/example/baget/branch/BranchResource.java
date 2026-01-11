package com.example.baget.branch;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/branch", produces = MediaType.APPLICATION_JSON_VALUE)
public class BranchResource {
    private final BranchRepository branchRepository;
    private final BranchService branchService;
    public BranchResource(final BranchRepository branchRepository, final BranchService branchService) {
        this.branchRepository = branchRepository;
        this.branchService = branchService;
    }
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsBranch(@RequestParam("name") String branchName) {
        boolean exists = branchRepository.findByName(branchName).isPresent();
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/manager")
    public List<BranchDTO> getAllowedBranches(Authentication authentication) {
        return branchService.allowedBranches(authentication.getName());
    }
}
