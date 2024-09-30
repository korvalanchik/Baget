package com.example.baget.jwt;

import com.example.baget.users.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UsersService userService;
    private final UserDetailsService userDetailsService;
    private final RoleRepository roleRepository;

    // Ін'єкція через конструктор
    @Autowired
    public AuthRestController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UsersService userService,
                          UserDetailsService userDetailsService,
                          RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDTO userDto) {
        try {
            userService.registerNewUser(userDto);
            return ResponseEntity.ok("User registered successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> loginUser(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials!");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        final String jwt = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            // Видалити префікс "Bearer " з токену
            String jwtToken = token.substring(7);

            // Отримати ім'я користувача з токену
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);

            // Завантажити користувача з бази даних
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Перевірити, чи токен дійсний
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                return ResponseEntity.ok("Token is valid");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Можна додати додаткові дії на стороні сервера, такі як інвалідовування токена
        // Але стандартно, клієнт просто видаляє токен зі своєї сторони

        // Наприклад, можна додати токен в blacklist на сервері
        // або встановити його строк дії як завершений, якщо у вас є такий механізм

        return ResponseEntity.ok("Logout successful");
    }

    @PostMapping("/password-recovery")
    public ResponseEntity<String> sendRecoveryLink(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendPasswordRecoveryEmail(email);
        return ResponseEntity.ok("Recovery link sent to your email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password successfully changed.");
    }

    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Boolean>> checkAdmin(Principal principal) {
        Map<String, Boolean> response = new HashMap<>();
        if (principal != null) {
            // Перевірка ролі користувача
            boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                    .stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            response.put("isAdmin", isAdmin);
        } else {
            response.put("isAdmin", false);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleDTO> roleDTOs = roles.stream()
                .map(role -> new RoleDTO(role.getId(), role.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDTOs);
    }

}
