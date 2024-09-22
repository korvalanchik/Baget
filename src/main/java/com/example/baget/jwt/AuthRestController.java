package com.example.baget.jwt;

import com.example.baget.users.User;
import com.example.baget.users.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UsersService userService;
    private final UserDetailsService userDetailsService;

    // Ін'єкція через конструктор
    @Autowired
    public AuthRestController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UsersService userService,
                          UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }
        userService.registerNewUser(user);
        return ResponseEntity.ok("User registered successfully!");
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

}
