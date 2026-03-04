package com.esprit.microservice.pidev.Controllers;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        try {
            // Handle moderation fields passed from admin panel
            User existing = service.getById(id);

            // Apply timeout if sent
            if (userDetails.isTimedOut() && userDetails.getTimeoutUntil() != null) {
                service.applyTimeout(id, userDetails.getTimeoutUntil());
                return ResponseEntity.ok(service.getById(id));
            }

            // Lift timeout
            if (!userDetails.isTimedOut() && existing.isTimedOut()) {
                service.liftTimeout(id);
                return ResponseEntity.ok(service.getById(id));
            }

            // Deactivate / reactivate
            if (!userDetails.isEnabled() && existing.isEnabled()) {
                service.deactivate(id);
                return ResponseEntity.ok(service.getById(id));
            }
            if (userDetails.isEnabled() && !existing.isEnabled()) {
                service.reactivate(id);
                return ResponseEntity.ok(service.getById(id));
            }

            // Regular profile update
            return ResponseEntity.ok(service.updateUser(id, userDetails));

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ── MODERATION ENDPOINTS ──────────────────────────────────────────────────

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportUser(@PathVariable Integer id) {
        try {
            User updated = service.reportUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("reportCount", updated.getReportCount());
            response.put("enabled", updated.isEnabled());
            response.put("message", updated.getReportCount() >= 3
                    ? "User has been auto-deactivated after 3 reports"
                    : "Report submitted. Warning email sent.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/timeout")
    public ResponseEntity<?> timeoutUser(@PathVariable Integer id,
                                         @RequestBody Map<String, String> body) {
        try {
            LocalDateTime until = LocalDateTime.parse(body.get("until"));
            User updated = service.applyTimeout(id, until);
            Map<String, Object> response = new HashMap<>();
            response.put("timedOut", updated.isTimedOut());
            response.put("timeoutUntil", updated.getTimeoutUntil());
            response.put("message", "Timeout applied until " + updated.getTimeoutUntil());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/lift-timeout")
    public ResponseEntity<?> liftTimeout(@PathVariable Integer id) {
        try {
            service.liftTimeout(id);
            return ResponseEntity.ok(Map.of("message", "Timeout lifted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<User> updateAvatar(@PathVariable Integer id,
                                             @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateAvatar(id, body.get("avatar")));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Integer id,
                                            @RequestBody PasswordChangeRequest request) {
        try {
            service.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        try {
            service.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Integer id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}