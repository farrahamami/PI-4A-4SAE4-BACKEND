package com.esprit.microservice.pidev.modules.subscription.controller;

import com.esprit.microservice.pidev.modules.subscription.domain.entities.PromoCode;
import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.esprit.microservice.pidev.modules.subscription.repository.PromoCodeRepository;
import com.esprit.microservice.pidev.modules.subscription.repository.UserSubscriptionRepository;
import com.esprit.microservice.pidev.modules.subscription.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PromoCodeRepository promoCodeRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final InvoiceService invoiceService;

    // ═══════════════════════════════════════
    //  VALIDER UN CODE PROMO
    // ═══════════════════════════════════════
    @GetMapping("/promo/validate/{code}")
    public ResponseEntity<Map<String, Object>> validatePromoCode(@PathVariable String code) {
        Map<String, Object> response = new HashMap<>();

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code).orElse(null);

        if (promo == null) {
            response.put("valid", false);
            response.put("message", "Code promo introuvable");
            return ResponseEntity.ok(response);
        }

        if (!promo.getIsActive()) {
            response.put("valid", false);
            response.put("message", "Code promo désactivé");
            return ResponseEntity.ok(response);
        }

        if (promo.getExpiresAt() != null && LocalDateTime.now().isAfter(promo.getExpiresAt())) {
            response.put("valid", false);
            response.put("message", "Code promo expiré");
            return ResponseEntity.ok(response);
        }

        if (promo.getCurrentUses() >= promo.getMaxUses()) {
            response.put("valid", false);
            response.put("message", "Code promo épuisé");
            return ResponseEntity.ok(response);
        }

        // ✅ Code valide
        response.put("valid", true);
        response.put("discountPercent", promo.getDiscountPercent());
        response.put("description", promo.getDescription());
        response.put("message", "-" + promo.getDiscountPercent() + "% appliqué !");
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════
    //  APPLIQUER UN CODE PROMO
    // ═══════════════════════════════════════
    @PostMapping("/promo/apply/{code}")
    public ResponseEntity<Map<String, Object>> applyPromoCode(@PathVariable String code) {
        Map<String, Object> response = new HashMap<>();

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code).orElse(null);
        if (promo == null || !promo.isValid()) {
            response.put("success", false);
            response.put("message", "Code promo invalide");
            return ResponseEntity.ok(response);
        }

        promo.incrementUses();
        promoCodeRepository.save(promo);

        response.put("success", true);
        response.put("discountPercent", promo.getDiscountPercent());
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════
    //  PAIEMENT SIMULÉ (100% LOCAL)
    // ═══════════════════════════════════════
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulatePayment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        // Simule un traitement de 1.5 secondes (effet réaliste)
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        String transactionId = "TXN-" + System.currentTimeMillis();

        response.put("success", true);
        response.put("transactionId", transactionId);
        response.put("message", "Paiement effectué avec succès");
        response.put("paymentMethod", "CARD_SIMULATED");

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════
    //  FACTURE PDF
    // ═══════════════════════════════════════
    @GetMapping("/invoice/{userSubscriptionId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long userSubscriptionId) {
        UserSubscription userSub = userSubscriptionRepository.findById(userSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'ID: " + userSubscriptionId));

        byte[] pdfBytes = invoiceService.generateInvoice(userSub);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "facture-prolance-" + userSubscriptionId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}