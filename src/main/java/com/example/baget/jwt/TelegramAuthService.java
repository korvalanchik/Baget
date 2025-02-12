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
import java.time.Instant;
import java.util.Arrays;
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

    public boolean validateTelegramData(String initData) {
        try {
            // Розбираємо initData
            Map<String, String> params = parseTelegramData(initData);

            // Перевіряємо наявність обов'язкових параметрів
            if (!params.containsKey("hash") || !params.containsKey("auth_date")) {
                return false; // Відсутні обов'язкові параметри
            }

            // Отримуємо hash із запиту
            String receivedHash = params.get("hash");
            params.remove("hash");

            // Перевіряємо час запиту (не більше 24 годин)
            long authDate;
            try {
                authDate = Long.parseLong(params.get("auth_date"));
            } catch (NumberFormatException e) {
                return false; // Невірний формат часу
            }

            long currentTime = Instant.now().getEpochSecond();
            if (currentTime - authDate > 86400) { // 86400 секунд = 24 години
                return false; // Запит прострочений
            }

            // Сортуємо параметри та створюємо рядок для підпису
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));

            // Крок 1: Створюємо HMAC-SHA256 ключ на основі токена бота
            byte[] secretKey = "WebAppData".getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] botTokenHash = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            // Крок 2: Створюємо HMAC-SHA256 підпис для даних
            SecretKeySpec dataKeySpec = new SecretKeySpec(botTokenHash, "HmacSHA256");
            mac.init(dataKeySpec);
            byte[] expectedHash = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            // Конвертуємо receivedHash з hex у byte[]
            byte[] receivedHashBytes = hexStringToByteArray(receivedHash);

            // Порівнюємо підписи
            return MessageDigest.isEqual(expectedHash, receivedHashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Допоміжний метод для конвертації hex-рядка в byte[]
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Метод для розбору initData
    private Map<String, String> parseTelegramData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(param -> param.split("=", 2)) // Розбиваємо лише на 2 частини
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts.length > 1 ? parts[1] : "" // Обробка відсутніх значень
                ));
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

//    public boolean validateTelegramData(String initData) {
//        try {
//            // Розбираємо initData
//            Map<String, String> params = parseTelegramData(initData);
//
//            // Перевіряємо наявність обов'язкових параметрів
//            if (!params.containsKey("hash") || !params.containsKey("auth_date")) {
//                return false; // Відсутні обов'язкові параметри
//            }
//
//            // Отримуємо hash із запиту
//            String receivedHash = params.remove("hash");
//
//            // Перевіряємо час запиту (не більше 24 годин)
//            long authDate;
//            try {
//                authDate = Long.parseLong(params.get("auth_date"));
//            } catch (NumberFormatException e) {
//                return false; // Невірний формат часу
//            }
//
//            long currentTime = Instant.now().getEpochSecond();
//            if (currentTime - authDate > 86400) { // 86400 секунд = 24 години
//                return false; // Запит прострочений
//            }
//
//            // Сортуємо параметри та створюємо рядок для підпису
//            String dataCheckString = params.entrySet().stream()
//                    .sorted(Map.Entry.comparingByKey())
//                    .map(entry -> entry.getKey() + "=" + entry.getValue())
//                    .collect(Collectors.joining("\n"));
//
//            // Крок 3: Генеруємо ключ HMAC-SHA256(botToken, "WebAppData")
//            byte[] botTokenBytes = botToken.getBytes(StandardCharsets.UTF_8);
//            SecretKeySpec keySpec1 = new SecretKeySpec(botTokenBytes, "HmacSHA256");
//
//            Mac mac1 = Mac.getInstance("HmacSHA256");
//            mac1.init(keySpec1);
//            byte[] secretKey = mac1.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
//
//            // Крок 4: Використовуємо отриманий ключ для підпису dataCheckString
//            SecretKeySpec keySpec2 = new SecretKeySpec(secretKey, "HmacSHA256");
//
//            Mac mac2 = Mac.getInstance("HmacSHA256");
//            mac2.init(keySpec2);
//            byte[] expectedHash = mac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
//
//            // Переводимо отриманий `receivedHash` з hex у byte[]
//            byte[] receivedHashBytes = hexStringToByteArray(receivedHash);
//
//            // Порівнюємо підписи
//            return MessageDigest.isEqual(expectedHash, receivedHashBytes);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Перетворює hex-рядок у масив байтів
//     */
//    private static byte[] hexStringToByteArray(String s) {
//        int len = s.length();
//        byte[] data = new byte[len / 2];
//        for (int i = 0; i < len; i += 2) {
//            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
//                    + Character.digit(s.charAt(i + 1), 16));
//        }
//        return data;
//    }
//
//    private Map<String, String> parseTelegramData(String initData) {
//        return Arrays.stream(initData.split("&"))
//                .map(param -> param.split("=", 2)) // Розбиваємо лише на 2 частини
//                .collect(Collectors.toMap(
//                        parts -> parts[0],
//                        parts -> parts.length > 1 ? parts[1] : "" // Обробка відсутніх значень
//                ));
//    }
}
