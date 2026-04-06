package com.esprit.microservice.adsservice.kafka;

import com.esprit.microservice.adsservice.dto.AdEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AD_EVENTS_TOPIC = "ad-events";
    private static final String MODERATION_LOGS_TOPIC = "moderation-logs";
    private static final String USER_ALERTS_TOPIC = "user-alerts";
    private static final String FLAGGED_USERS_TOPIC = "flagged_users";

    public void sendAdEvent(AdEvent event) {
        try {
            log.info("[KAFKA] Sending ad event to {}: {}", AD_EVENTS_TOPIC, event);
            kafkaTemplate.send(AD_EVENTS_TOPIC, event.getAdId().toString(), event);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to send ad event: {}", e.getMessage(), e);
        }
    }

    public void sendModerationLog(Long adId, Long userId, String userEmail, String title, String description, String violation, String categoryCode) {
        try {
            Map<String, Object> logData = Map.of(
                "adId", adId,
                "userId", userId != null ? userId : 0L,
                "userEmail", userEmail != null ? userEmail : "unknown",
                "title", title != null ? title : "",
                "description", description != null ? description : "",
                "violation", violation,
                "categoryCode", categoryCode,
                "timestamp", LocalDateTime.now().toString()
            );
            log.info("[KAFKA] Sending moderation log to {}: Ad {} - User {} - {}", 
                    MODERATION_LOGS_TOPIC, adId, userEmail, violation);
            kafkaTemplate.send(MODERATION_LOGS_TOPIC, adId.toString(), logData);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to send moderation log: {}", e.getMessage(), e);
        }
    }

    public void sendUserAlert(Long userId, String userEmail, String message) {
        try {
            Map<String, Object> alert = Map.of(
                "userId", userId,
                "userEmail", userEmail != null ? userEmail : "unknown",
                "message", message,
                "priority", "HIGH",
                "timestamp", LocalDateTime.now().toString()
            );
            log.warn("[KAFKA] Sending HIGH PRIORITY alert to {}: User {} - {}", USER_ALERTS_TOPIC, userId, message);
            kafkaTemplate.send(USER_ALERTS_TOPIC, userId.toString(), alert);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to send user alert: {}", e.getMessage(), e);
        }
    }

    public void sendUserFlaggedEvent(Long userId, String email, int violationCount) {
        try {
            Map<String, Object> event = Map.of(
                "userId", userId,
                "email", email != null ? email : "unknown",
                "violationCount", violationCount,
                "timestamp", LocalDateTime.now().toString(),
                "status", "FLAGGED"
            );
            log.warn("[KAFKA] Sending FLAGGED event to {}: User {} ({}) with {} violations", 
                    FLAGGED_USERS_TOPIC, email, userId, violationCount);
            kafkaTemplate.send(FLAGGED_USERS_TOPIC, userId.toString(), event);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to send user flagged event: {}", e.getMessage(), e);
        }
    }
}
