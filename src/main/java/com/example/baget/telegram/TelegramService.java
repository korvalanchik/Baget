package com.example.baget.telegram;

import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TelegramService {

    @Value("${bot.token}")
    private String botToken;

    @Value("${CHAT_ID}")
    private String adminChatId;

    private final UsersRepository userRepository;

    public TelegramService(UsersRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void sendMessage(String messageJson) {
        try {
            // Парсимо JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> messageMap = objectMapper.readValue(messageJson, new TypeReference<>() {});
            String text = messageMap.getOrDefault("message", "").toString();

            // 1. Адміну
            sendMessageToChat(text, Long.valueOf(adminChatId));

            // 2. Клієнту (авторизованому користувачу)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                String username = authentication.getName();

                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null && user.getTelegramId() != null) {
                    sendMessageToChat(text, user.getTelegramId());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToChat(String messageText, Long chatId) {
        try {
            String telegram_API_URL = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> requestBody = Map.of(
                    "chat_id", chatId,
                    "text", messageText,
                    "parse_mode", "HTML"
            );

            restTemplate.postForObject(telegram_API_URL, requestBody, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
