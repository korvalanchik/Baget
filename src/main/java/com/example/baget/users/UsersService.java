package com.example.baget.users;

import com.example.baget.passwordrecoverytoken.PasswordRecoveryToken;
import com.example.baget.passwordrecoverytoken.PasswordRecoveryTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {
    private final UsersRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Ін'єкція через конструктор
    @Autowired
    public UsersService(UsersRepository userRepository,
                        RoleRepository roleRepository, PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
                        EmailService emailService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordRecoveryTokenRepository = passwordRecoveryTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerNewUser(UserRegistrationDTO userDto) {
        // Перевірка існування користувача за username або email
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        // Отримання ролей за їх ідентифікаторами
        List<Role> roles = roleRepository.findAllById(userDto.getRoles());
        // Створення нового користувача
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRoles(roles);
        // Збереження користувача
        userRepository.save(user);
    }

    public void sendPasswordRecoveryEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = UUID.randomUUID().toString(); // Генеруємо токен для відновлення
        String recoveryLink = "https://ramarnya.vercel.app/auth/passreset.html?token=" + token;

        // Логіка для збереження токену у БД (для подальшої валідації)
        saveRecoveryToken(user, token);

        // Надсилання email з посиланням для відновлення паролю
        emailService.sendEmail(email,"Password Recovery", "Click on the link to reset your password: " + recoveryLink);
    }

    private void saveRecoveryToken(User user, String token) {
        // Логіка для збереження токену у базі даних (можна створити окрему сутність PasswordRecoveryToken)
        PasswordRecoveryToken recoveryToken = new PasswordRecoveryToken();
        recoveryToken.setUser(user);
        recoveryToken.setToken(token);
        recoveryToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // Токен діє 1 годину
        passwordRecoveryTokenRepository.save(recoveryToken);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordRecoveryToken recoveryToken = passwordRecoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (recoveryToken.isExpired()) {
            throw new IllegalArgumentException("Token has expired");
        }

        User user = recoveryToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Видалити токен після успішної зміни паролю
        passwordRecoveryTokenRepository.delete(recoveryToken);
    }

}
