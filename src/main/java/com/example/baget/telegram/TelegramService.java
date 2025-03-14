package com.example.baget.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${bot.token}")
    private String botToken;

    @Value("${CHAT_ID}")
    private String chatId;

    public void sendMessage(String message) {
        String telegram_API_URL = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", message);
        requestBody.put("parse_mode", "Markdown"); // Або "HTML"

        restTemplate.postForObject(telegram_API_URL, requestBody, String.class);
    }

}
