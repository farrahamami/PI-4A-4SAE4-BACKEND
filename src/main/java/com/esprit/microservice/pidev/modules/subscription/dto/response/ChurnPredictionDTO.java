package com.esprit.microservice.pidev.modules.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnPredictionDTO {

    private Long userSubscriptionId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String planName;
    private String planType;       // FREELANCER or CLIENT
    private String billingCycle;
    private BigDecimal amountPaid;

    // Subscription status
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenew;
    private long daysRemaining;

    // Usage metrics
    private Integer currentProjects;
    private Integer maxProjects;
    private Integer currentProposals;
    private Integer maxProposals;
    private double projectsUsagePercent;
    private double proposalsUsagePercent;

    // ═══ AI CHURN PREDICTION ═══
    private int churnScore;            // 0-100 (0 = safe, 100 = will churn)
    private String riskLevel;          // LOW, MEDIUM, HIGH, CRITICAL
    private List<ChurnFactor> factors; // Detailed breakdown of why
    private String aiSummary;          // Human-readable recommendation
    private String suggestedAction;    // What admin should do

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChurnFactor {
        private String name;
        private String description;
        private int impact;         // Points added to churn score
        private String severity;    // LOW, MEDIUM, HIGH
    }
}