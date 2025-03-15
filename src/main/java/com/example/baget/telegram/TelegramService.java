package com.example.baget.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TelegramService {

    @Value("${bot.token}")
    private String botToken;

    @Value("${CHAT_ID}")
    private String chatId;

    public void sendMessage(String messageJson) {
        try {
            // Парсимо JSON у Map<String, Object>
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> messageMap = objectMapper.readValue(messageJson, new TypeReference<>() {});

            // Отримуємо текст повідомлення (переконавшись, що ключ існує)
            String text = messageMap.getOrDefault("message", "").toString();

            String telegram_API_URL = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> requestBody = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML" // Або "Markdown"
            );

            restTemplate.postForObject(telegram_API_URL, requestBody, String.class);
        } catch (Exception e) {
            e.printStackTrace(); // Логування помилки
        }
    }
}