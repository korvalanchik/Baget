package com.example.baget.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${bot.token}")
    private String TOKEN;
    private final String CHAT_ID = "5217802119";
    private final String TELEGRAM_API_URL = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";

    public void sendMessage(String message) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", CHAT_ID);
        requestBody.put("text", message);
        requestBody.put("parse_mode", "Markdown"); // Або "HTML"

        restTemplate.postForObject(TELEGRAM_API_URL, requestBody, String.class);
    }

}
