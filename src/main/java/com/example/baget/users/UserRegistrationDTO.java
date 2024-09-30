package com.example.baget.users;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRegistrationDTO {
    private String username;
    private String email;
    private String password;
    private List<Long> roles; // Список ідентифікаторів ролей
}
