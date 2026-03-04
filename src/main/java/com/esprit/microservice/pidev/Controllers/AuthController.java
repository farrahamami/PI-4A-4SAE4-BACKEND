package com.esprit.microservice.pidev.Controllers;

import com.esprit.microservice.pidev.Entities.Role;
import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import com.esprit.microservice.pidev.dto.AuthRequest;
import com.esprit.microservice.pidev.dto.AuthResponse;
import com.esprit.microservice.pidev.dto.RegisterRequest;
import com.esprit.microservice.pidev.Services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Register and login users")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;


    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }
    @Operation(summary = "Login a user")
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        authService.register(request);
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
