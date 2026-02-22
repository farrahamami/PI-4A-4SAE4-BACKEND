package com.esprit.microservice.pidev.modules.subscription.controller;

import com.esprit.microservice.pidev.modules.subscription.dto.response.ChurnPredictionDTO;
import com.esprit.microservice.pidev.modules.subscription.service.ChurnPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/churn-prediction")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "AI Churn Prediction", description = "AI-powered churn risk analysis for subscriptions")
public class ChurnPredictionController {

    private final ChurnPredictionService churnPredictionService;

    @GetMapping
    @Operation(summary = "Get churn predictions for all active subscriptions")
    public ResponseEntity<List<ChurnPredictionDTO>> getAllPredictions() {
        List<ChurnPredictionDTO> predictions = churnPredictionService.getAllChurnPredictions();
        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/high-risk")
    @Operation(summary = "Get only HIGH and CRITICAL risk subscriptions")
    public ResponseEntity<List<ChurnPredictionDTO>> getHighRiskSubscriptions() {
        List<ChurnPredictionDTO> highRisk = churnPredictionService.getHighRiskSubscriptions();
        return ResponseEntity.ok(highRisk);
    }

    @GetMapping("/{userSubscriptionId}")
    @Operation(summary = "Get churn prediction for a specific subscription")
    public ResponseEntity<ChurnPredictionDTO> getPrediction(@PathVariable Long userSubscriptionId) {
        ChurnPredictionDTO prediction = churnPredictionService.getChurnPrediction(userSubscriptionId);
        return ResponseEntity.ok(prediction);
    }
}