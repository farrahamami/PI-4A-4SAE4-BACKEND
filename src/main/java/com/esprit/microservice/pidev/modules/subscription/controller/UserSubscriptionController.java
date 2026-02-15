package com.esprit.microservice.pidev.modules.subscription.controller;

import com.esprit.microservice.pidev.modules.subscription.dto.request.SubscribeRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.UserSubscriptionResponse;
import com.esprit.microservice.pidev.modules.subscription.service.UserSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "User Subscription Management", description = "API pour gérer les abonnements des utilisateurs")
public class UserSubscriptionController {

    private final UserSubscriptionService userSubscriptionService;

    @PostMapping("/subscribe")
    @Operation(summary = "Souscrire à un plan d'abonnement")
    public ResponseEntity<UserSubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        UserSubscriptionResponse response = userSubscriptionService.subscribe(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Récupérer l'abonnement actif d'un utilisateur")
    public ResponseEntity<UserSubscriptionResponse> getActiveSubscription(@PathVariable Long userId) {
        UserSubscriptionResponse subscription = userSubscriptionService.getActiveSubscription(userId);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/user/{userId}/history")
    @Operation(summary = "Récupérer l'historique des abonnements d'un utilisateur")
    public ResponseEntity<List<UserSubscriptionResponse>> getUserSubscriptionHistory(@PathVariable Long userId) {
        List<UserSubscriptionResponse> history = userSubscriptionService.getUserSubscriptionHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un abonnement par ID")
    public ResponseEntity<UserSubscriptionResponse> getUserSubscriptionById(@PathVariable Long id) {
        UserSubscriptionResponse subscription = userSubscriptionService.getUserSubscriptionById(id);
        return ResponseEntity.ok(subscription);
    }

    @PatchMapping("/user/{userId}/cancel")
    @Operation(summary = "Annuler un abonnement")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long userId) {
        userSubscriptionService.cancelSubscription(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}/renew")
    @Operation(summary = "Renouveler un abonnement")
    public ResponseEntity<UserSubscriptionResponse> renewSubscription(@PathVariable Long userId) {
        UserSubscriptionResponse response = userSubscriptionService.renewSubscription(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/user/{userId}/auto-renew")
    @Operation(summary = "Activer/Désactiver le renouvellement automatique")
    public ResponseEntity<Void> toggleAutoRenew(
            @PathVariable Long userId,
            @RequestParam Boolean autoRenew) {
        userSubscriptionService.toggleAutoRenew(userId, autoRenew);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/increment-projects")
    @Operation(summary = "Incrémenter le compteur de projets")
    public ResponseEntity<Void> incrementProjectCount(@PathVariable Long userId) {
        userSubscriptionService.incrementProjectCount(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/increment-proposals")
    @Operation(summary = "Incrémenter le compteur de propositions")
    public ResponseEntity<Void> incrementProposalCount(@PathVariable Long userId) {
        userSubscriptionService.incrementProposalCount(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/decrement-projects")
    @Operation(summary = "Décrémenter le compteur de projets")
    public ResponseEntity<Void> decrementProjectCount(@PathVariable Long userId) {
        userSubscriptionService.decrementProjectCount(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/decrement-proposals")
    @Operation(summary = "Décrémenter le compteur de propositions")
    public ResponseEntity<Void> decrementProposalCount(@PathVariable Long userId) {
        userSubscriptionService.decrementProposalCount(userId);
        return ResponseEntity.noContent().build();
    }
}