package com.esprit.microservice.pidev.Services;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User getById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    // UPDATE USER PROFILE
    public User updateUser(Integer id, User userDetails) {
        User user = getById(id);

        if (userDetails.getName() != null)      user.setName(userDetails.getName());
        if (userDetails.getLastName() != null)  user.setLastName(userDetails.getLastName());
        if (userDetails.getEmail() != null)     user.setEmail(userDetails.getEmail());
        if (userDetails.getBirthDate() != null) user.setBirthDate(userDetails.getBirthDate());

        return repo.save(user);
    }

    // ── TIMEOUT ──────────────────────────────────────────────────────────────
    public User applyTimeout(Integer id, LocalDateTime until) {
        User user = getById(id);
        user.setTimedOut(true);
        user.setTimeoutUntil(until);
        return repo.save(user);
    }

    public User liftTimeout(Integer id) {
        User user = getById(id);
        user.setTimedOut(false);
        user.setTimeoutUntil(null);
        return repo.save(user);
    }

    // ── REPORT ───────────────────────────────────────────────────────────────
    public User reportUser(Integer id) {
        User user = getById(id);
        int newCount = user.getReportCount() + 1;
        user.setReportCount(newCount);

        // Send warning email on first report
        if (newCount == 1) {
            sendEmail(user.getEmail(),
                    "Warning: Report filed against your account",
                    "Hello " + user.getName() + ",\n\n" +
                            "A report has been filed against your account for violating our community guidelines.\n\n" +
                            "Please note: if your account accumulates 3 reports, it will be automatically deactivated.\n\n" +
                            "If you believe this is a mistake, please contact our support team.\n\n" +
                            "The Platform Team"
            );
        }

        // Auto-deactivate at 3 reports
        if (newCount >= 3) {
            user.setEnabled(false);
            sendEmail(user.getEmail(),
                    "Your account has been deactivated",
                    "Hello " + user.getName() + ",\n\n" +
                            "Your account has been deactivated due to receiving 3 or more reports.\n\n" +
                            "If you believe this is a mistake, please contact our support team.\n\n" +
                            "The Platform Team"
            );
        }

        return repo.save(user);
    }

    // ── DEACTIVATE / REACTIVATE ───────────────────────────────────────────────
    public void deactivate(Integer id) {
        User user = getById(id);
        user.setEnabled(false);
        repo.save(user);
    }

    public void reactivate(Integer id) {
        User user = getById(id);
        user.setEnabled(true);
        user.setReportCount(0); // reset reports on reactivation
        repo.save(user);
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        User user = getById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);
    }

    public User updateAvatar(Integer id, String avatarBase64) {
        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatar(avatarBase64);
        return repo.save(user);
    }

    public void deleteUser(Integer id) {
        repo.delete(getById(id));
    }

    // ── EMAIL HELPER ──────────────────────────────────────────────────────────
    private void sendEmail(String to, String subject, String body) {
        if (mailSender == null) return; // skip if mail not configured
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}