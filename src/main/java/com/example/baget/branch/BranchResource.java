package com.example.baget.branch;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/branch", produces = MediaType.APPLICATION_JSON_VALUE)
public class BranchResource {
    private final BranchRepository branchRepository;
    public BranchResource(final BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsBranch(@RequestParam("name") String branchName) {
        boolean exists = branchRepository.findByName(branchName).isPresent();
        return ResponseEntity.ok(exists);
    }
}
