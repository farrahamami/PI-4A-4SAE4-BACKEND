package com.esprit.microservice.pidev.Services;

import com.esprit.microservice.pidev.Entities.Role;
import com.esprit.microservice.pidev.dto.AuthRequest;
import com.esprit.microservice.pidev.dto.AuthResponse;
import com.esprit.microservice.pidev.dto.RegisterRequest;
import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Deactivated account → 403
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "deactivated");
        }

        // Timed out account → 423
        if (user.isTimedOut() && user.getTimeoutUntil() != null) {
            if (LocalDateTime.now().isBefore(user.getTimeoutUntil())) {
                throw new ResponseStatusException(HttpStatus.valueOf(423), "suspended");
            } else {
                // Timeout expired — lift automatically
                user.setTimedOut(false);
                user.setTimeoutUntil(null);
                userRepository.save(user);
            }
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                user.getId(),
                token,
                user.getRole().name()
        );
    }

    public void register(RegisterRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBirthDate(request.getBirthDate());
        user.setEnabled(true);

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            role = Role.CLIENT;
        }
        user.setRole(role);

        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}