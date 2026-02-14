package com.esprit.microservice.pidev.modules.subscription.dto.request;


import com.esprit.microservice.pidev.modules.subscription.domain.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionRequest {

    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    private String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    private BillingCycle billingCycle;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    @Min(value = 1, message = "Le nombre de projets doit être au moins 1")
    private Integer maxProjects;

    @Min(value = 1, message = "Le nombre de propositions doit être au moins 1")
    private Integer maxProposals;

    @Min(value = 1, message = "Le nombre de jobs actifs doit être au moins 1")
    private Integer maxActiveJobs;

    private Boolean featuredListing;
    private Boolean prioritySupport;
    private Boolean analyticsAccess;
    private Boolean isActive;
}