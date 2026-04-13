package com.esprit.userservice.Controllers;

import com.esprit.userservice.Entities.User;
import com.esprit.userservice.Repositories.UserRepository;
import com.esprit.userservice.dto.AuthRequest;
import com.esprit.userservice.dto.AuthResponse;
import com.esprit.userservice.dto.RegisterRequest;
import com.esprit.userservice.Services.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Register and login users")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    private final String SECRET = "super-secret-key-that-is-at-least-32-chars!";
    private final Key jwtKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService    = authService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Login a user")
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Please check your email to verify your account."));
    }

    // ── Face login — called after face recognition matches an enrolled email ──
    @Operation(summary = "Login via face recognition")
    @PostMapping("/face-login")
    public ResponseEntity<?> faceLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000L)) // 24h
                .signWith(jwtKey)
                .compact();

        return ResponseEntity.ok(Map.of(
                "id",       user.getId(),
                "email",    user.getEmail(),
                "name",     user.getName(),
                "lastName", user.getLastName(),
                "role",     user.getRole().name(),
                "token",    token
        ));
    }

    // ── Verify email — called when user clicks link in email ──────────────────
    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            authService.verifyEmail(token);
            response.sendRedirect("http://localhost:4200/#/login?verified=true");
        } catch (Exception e) {
            response.sendRedirect("http://localhost:4200/#/login?verified=false&reason=" + e.getMessage());
        }
    }

    // ── Resend verification email ─────────────────────────────────────────────
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        try {
            authService.resendVerification(email);
            return ResponseEntity.ok(Map.of("message", "Verification email sent!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Get all registered users")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return authService.getAllUsers();
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
    }
}