package com.example.baget.users;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Long telegramId;
    private List<String> roles; // Список ідентифікаторів ролей
}
