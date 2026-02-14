package com.esprit.microservice.pidev.modules.subscription.mapper;


import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.esprit.microservice.pidev.modules.subscription.dto.response.UserSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class UserSubscriptionMapper {

    private final SubscriptionMapper subscriptionMapper;

    public UserSubscriptionResponse toResponse(UserSubscription userSubscription) {
        Long daysRemaining = calculateDaysRemaining(userSubscription.getEndDate());

        return UserSubscriptionResponse.builder()
                .id(userSubscription.getId())
                .userId(userSubscription.getUserId())
                .subscription(subscriptionMapper.toResponse(userSubscription.getSubscription()))
                .status(userSubscription.getStatus())
                .startDate(userSubscription.getStartDate())
                .endDate(userSubscription.getEndDate())
                .cancelledAt(userSubscription.getCancelledAt())
                .autoRenew(userSubscription.getAutoRenew())
                .currentProjects(userSubscription.getCurrentProjects())
                .currentProposals(userSubscription.getCurrentProposals())
                .amountPaid(userSubscription.getAmountPaid())
                .paymentMethod(userSubscription.getPaymentMethod())
                .transactionId(userSubscription.getTransactionId())
                .createdAt(userSubscription.getCreatedAt())
                .updatedAt(userSubscription.getUpdatedAt())
                .daysRemaining(daysRemaining)
                .isExpiringSoon(daysRemaining != null && daysRemaining <= 30)
                .build();
    }

    private Long calculateDaysRemaining(LocalDateTime endDate) {
        if (endDate == null) return null;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endDate)) return 0L;
        return ChronoUnit.DAYS.between(now, endDate);
    }
}