package com.example.baget.telegram;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
public class TelegramResource {
    private final TelegramService telegramService;

    public TelegramResource(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        telegramService.sendMessage(message);
        return "Повідомлення відправлено у майстерню!";
    }

}
