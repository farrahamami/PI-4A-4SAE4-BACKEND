package com.esprit.microservice.pidev.modules.subscription.dto.response;

import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponse {

    private Long id;

    private Integer userId;
    private String userName;
    private String userLastName;
    private String userEmail;

    private SubscriptionResponse subscription;

    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime cancelledAt;

    private Boolean autoRenew;

    private Integer currentProjects;
    private Integer currentProposals;

    private BigDecimal amountPaid;
    private String paymentMethod;
    private String transactionId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long daysRemaining;
    private Boolean isExpiringSoon;
}