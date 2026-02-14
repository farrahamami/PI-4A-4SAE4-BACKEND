package com.esprit.microservice.pidev.modules.subscription.dto.response;


import com.esprit.microservice.pidev.modules.subscription.domain.enums.BillingCycle;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
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
public class SubscriptionResponse {

    private Long id;
    private String name;
    private SubscriptionType type;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private String description;

    private Integer maxProjects;
    private Integer maxProposals;
    private Integer maxActiveJobs;

    private Boolean featuredListing;
    private Boolean prioritySupport;
    private Boolean analyticsAccess;

    private Boolean isActive;
    private Long activeSubscribersCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}