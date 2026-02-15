package com.esprit.microservice.pidev.modules.subscription.controller;

import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
import com.esprit.microservice.pidev.modules.subscription.dto.request.CreateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.request.UpdateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.SubscriptionResponse;
import com.esprit.microservice.pidev.modules.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Subscription Management", description = "API pour gérer les plans d'abonnement")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Créer un nouveau plan d'abonnement")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les plans d'abonnement")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        List<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/active")
    @Operation(summary = "Récupérer tous les plans actifs")
    public ResponseEntity<List<SubscriptionResponse>> getActiveSubscriptions() {
        List<SubscriptionResponse> subscriptions = subscriptionService.getActiveSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Récupérer les plans par type (CLIENT ou FREELANCER)")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByType(
            @PathVariable SubscriptionType type) {
        List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsByType(type);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un plan par ID")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable Long id) {
        SubscriptionResponse subscription = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(subscription);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un plan d'abonnement")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscription(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un plan")
    public ResponseEntity<Void> deactivateSubscription(@PathVariable Long id) {
        subscriptionService.deactivateSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activer un plan")
    public ResponseEntity<Void> activateSubscription(@PathVariable Long id) {
        subscriptionService.activateSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement un plan")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}