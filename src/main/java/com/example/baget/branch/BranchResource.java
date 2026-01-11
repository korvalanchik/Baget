package com.example.baget.branch;

import com.example.baget.users.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/manager")
    public List<BranchDTO> getAllowedBranches(Authentication auth) {
        User user = (User) auth.getPrincipal();

        return user.getAllowedBranches().stream()
                .map(b -> new BranchDTO(b.getBranchNo(), b.getName()))
                .toList();
    }
}
