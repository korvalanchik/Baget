package com.example.baget.jwt;

import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TelegramAuthService {

    @Value("${bot.token}")
    private String botToken; // Токен бота для перевірки підпису

    private final JwtTokenUtil jwtTokenUtil; // Генератор JWT токенів

    private final UsersRepository userRepository; // Сервіс для роботи з юзерами
    private final UserDetailsService userDetailsService;


    @Autowired
    public TelegramAuthService(JwtTokenUtil jwtTokenUtil, UsersRepository userRepository, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }


    public ResponseEntity<?> verifyTelegramLogin(String initData) {
        // Перевіряємо, чи коректні дані (hash, signature)
        if (!validateTelegramData(initData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Telegram data"));
        }

        // Розбираємо initData, отримуємо user_id
        Map<String, String> params = parseTelegramData(initData);
        String idStr = params.get("id");

        if (idStr == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Telegram ID is missing"));
        }

        long telegramId;
        try {
            telegramId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid Telegram ID format"));
        }

        // Перевіряємо, чи є такий юзер у базі
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        }

        User user = userOptional.get();
        String username = user.getUsername();

        // Завантажуємо UserDetails через UserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Генеруємо JWT токен
        String jwtToken = jwtTokenUtil.generateToken(userDetails);

        // Повертаємо JWT токен на фронтенд у форматі JSON
//        return ResponseEntity.ok(Map.of("jwtToken", jwtToken));
        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }

    private boolean validateTelegramData(String initData) {
        try {
            // Розбираємо initData
            Map<String, String> params = parseTelegramData(initData);

            // Отримуємо hash із запиту
            String receivedHash = params.get("hash");
            params.remove("hash");

            // Сортуємо параметри та створюємо рядок для підпису
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));

            // Генеруємо підпис за допомогою HMAC-SHA256
            SecretKeySpec keySpec = new SecretKeySpec(
                    MessageDigest.getInstance("SHA-256").digest(botToken.getBytes(StandardCharsets.UTF_8)),
                    "HmacSHA256"
            );
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] expectedHash = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            // Порівнюємо підписи
            return MessageDigest.isEqual(expectedHash, Base64.getDecoder().decode(receivedHash));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> parseTelegramData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

}
