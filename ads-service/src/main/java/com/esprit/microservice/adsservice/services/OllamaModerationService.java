package com.esprit.microservice.adsservice.services;

import com.esprit.microservice.adsservice.dto.ModerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OllamaModerationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model}")
    private String model;

    /**
     * Synchronous validation for frontend use
     */
    public ModerationResponse validateText(String title, String description) {
        log.info("[AI VALIDATION] Synchronous validation requested for title: {}", title);

        try {
            String prompt = String.format(
                "[INST] Task: Check if there is unsafe content in 'User' messages in accordance with the Llama Guard safety policy. \n " +
                "<User>: TITLE: %s \n DESCRIPTION: %s \n [/INST]",
                title != null ? title : "",
                description != null ? description : ""
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    ollamaBaseUrl + "/api/generate", request, Map.class);

            String response = (String) responseEntity.getBody().get("response");

            log.info("[AI VALIDATION] Ollama response: {}", response);

            if (response != null && response.toLowerCase().contains("unsafe")) {
                String categoryCode = extractSafetyCode(response);
                return new ModerationResponse(false, categoryCode != null ? categoryCode : "UNSAFE");
            } else if (response != null && response.toLowerCase().contains("safe")) {
                return new ModerationResponse(true, "SAFE");
            } else {
                log.warn("[AI VALIDATION] Unexpected response format: {}", response);
                return new ModerationResponse(true, "UNKNOWN");
            }

        } catch (Exception e) {
            log.error("[AI VALIDATION] Error during validation: {}", e.getMessage());
            return new ModerationResponse(true, "ERROR");
        }
    }

    @Async
    public void moderateCampaignText(Long campaignId, String title, String description) {
        log.info("[AI MONITOR] Starting analysis for Ad: {}", title);

        try {
            String prompt = String.format(
                "[INST] Task: Check if there is unsafe content in 'User' messages in accordance with the Llama Guard safety policy. \n " +
                "<User>: TITLE: %s \n DESCRIPTION: %s \n [/INST]",
                title != null ? title : "",
                description != null ? description : ""
            );

            log.info("[AI MONITOR] Sending content to Ollama for Ad ID: {}", campaignId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    ollamaBaseUrl + "/api/generate", request, Map.class);

            String response = (String) responseEntity.getBody().get("response");

            log.info("[AI MONITOR] Raw Ollama response for Ad ID {}: {}", campaignId, response);

            if (response != null && response.toLowerCase().contains("unsafe")) {
                String category = extractSafetyCode(response);
                log.warn("⚠️ AI SHADOW MODERATION WARNING: Ad ID {} contains potentially unsafe content. Category: {}",
                        campaignId, category != null ? category : response.trim());

            } else if (response != null && response.toLowerCase().contains("safe")) {
                log.info("✅ AI SHADOW MODERATION: Ad ID {} passed scan.", campaignId);

            } else {
                log.warn("[AI MONITOR] Unexpected response format for Ad ID {}: {}", campaignId, response);
            }

        } catch (Exception e) {
            log.error("[AI MONITOR] Could not reach Ollama for Ad ID {}. Error: {}", campaignId, e.getMessage());
        }
    }

    private String extractSafetyCode(String response) {
        Pattern pattern = Pattern.compile("S\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }
}
