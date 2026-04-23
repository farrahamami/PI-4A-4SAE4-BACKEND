package tn.esprit.microservice.subscriptionservice.subscription.controller;

import tn.esprit.microservice.subscriptionservice.subscription.domain.entities.UserSubscription;
import tn.esprit.microservice.subscriptionservice.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/churn-prediction")
@RequiredArgsConstructor
public class ChurnPredictionController {

    private final UserSubscriptionRepository repo;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        repo.findAll().forEach(us -> {
            try {
                result.add(buildPrediction(us));
            } catch (Exception e) {
                // Skip entrées corrompues sans faire crasher tout le endpoint
            }
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<Map<String, Object>>> getHighRisk() {
        List<Map<String, Object>> result = new ArrayList<>();
        repo.findAll().forEach(us -> {
            try {
                Map<String, Object> p = buildPrediction(us);
                if ((int) p.get("churnScore") >= 60) result.add(p);
            } catch (Exception e) {
                // Skip entrées corrompues
            }
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(us -> ResponseEntity.ok(buildPrediction(us)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> buildPrediction(UserSubscription us) {
        // ✅ FIX 1 : NullPointerException si endDate est null
        long daysRemaining = us.getEndDate() != null
                ? ChronoUnit.DAYS.between(LocalDateTime.now(), us.getEndDate())
                : 0;

        // ✅ FIX 2 : NullPointerException si subscription est null
        if (us.getSubscription() == null) {
            throw new IllegalStateException("UserSubscription #" + us.getId() + " has no linked subscription");
        }

        int score = calculateChurnScore(us, daysRemaining);
        String riskLevel = score >= 75 ? "CRITICAL" : score >= 60 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";

        Map<String, Object> p = new HashMap<>();
        p.put("userSubscriptionId", us.getId());
        p.put("userId",        us.getUserId());
        p.put("userName",      "User #" + us.getUserId());
        p.put("planName",      us.getSubscription().getName() != null ? us.getSubscription().getName() : "Unknown");
        p.put("planType",      us.getSubscription().getType() != null ? us.getSubscription().getType().toString() : "");
        p.put("billingCycle",  us.getSubscription().getBillingCycle() != null ? us.getSubscription().getBillingCycle().toString() : "");
        p.put("amountPaid",    us.getAmountPaid() != null ? us.getAmountPaid() : 0);
        p.put("status",        us.getStatus() != null ? us.getStatus().toString() : "UNKNOWN");
        p.put("startDate",     us.getStartDate() != null ? us.getStartDate().toString() : null);
        // ✅ FIX 3 : endDate.toString() plantait si null
        p.put("endDate",       us.getEndDate() != null ? us.getEndDate().toString() : null);
        p.put("autoRenew",     us.getAutoRenew() != null ? us.getAutoRenew() : false);
        p.put("daysRemaining", daysRemaining);
        p.put("currentProjects",  us.getCurrentProjects() != null ? us.getCurrentProjects() : 0);
        p.put("maxProjects",      us.getSubscription().getMaxProjects());
        p.put("currentProposals", us.getCurrentProposals() != null ? us.getCurrentProposals() : 0);
        p.put("maxProposals",     us.getSubscription().getMaxProposals());
        p.put("churnScore",    score);
        p.put("riskLevel",     riskLevel);
        p.put("factors",       new ArrayList<>());
        p.put("aiSummary",     "Score de risque basé sur l'activité et l'expiration");
        p.put("suggestedAction", score >= 60 ? "Contacter l'utilisateur" : "Surveiller l'activité");

        // ═══ Champs attendus par le frontend Angular ═══

        // Email utilisateur (placeholder — à remplacer par un appel Feign vers user-service)
        p.put("userEmail", "user" + us.getUserId() + "@prolance.tn");

        // Pourcentages d'usage (calculés)
        int maxProjects  = us.getSubscription().getMaxProjects()  != null ? us.getSubscription().getMaxProjects()  : 1;
        int maxProposals = us.getSubscription().getMaxProposals() != null ? us.getSubscription().getMaxProposals() : 1;
        int curProjects  = us.getCurrentProjects()  != null ? us.getCurrentProjects()  : 0;
        int curProposals = us.getCurrentProposals() != null ? us.getCurrentProposals() : 0;

        p.put("projectsUsagePercent",  maxProjects  > 0 ? (curProjects  * 100 / maxProjects)  : 0);
        p.put("proposalsUsagePercent", maxProposals > 0 ? (curProposals * 100 / maxProposals) : 0);

        // Probabilité ML brute (0.0 – 1.0) dérivée du churnScore
        // 💡 À remplacer par mlResult.getChurnProbability() quand le modèle ML sera branché
        p.put("churnProbability", score / 100.0);

        // Version du modèle
        p.put("modelVersion", "rule-based-v1");

        return p;
    }

    private int calculateChurnScore(UserSubscription us, long daysRemaining) {
        int score = 0;
        if (daysRemaining < 7)        score += 40;
        else if (daysRemaining < 30)  score += 20;
        // ✅ FIX 4 : autoRenew peut être null (unboxing NPE)
        if (Boolean.FALSE.equals(us.getAutoRenew())) score += 30;
        // ✅ FIX 5 : currentProjects peut être null
        if (us.getCurrentProjects() == null || us.getCurrentProjects() == 0) score += 20;
        return Math.min(score, 100);
    }
}