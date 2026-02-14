package com.esprit.microservice.pidev.modules.subscription.dto.request;


import com.esprit.microservice.pidev.modules.subscription.domain.enums.BillingCycle;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Le nom de l'abonnement est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    private String name;

    @NotNull(message = "Le type d'abonnement est obligatoire")
    private SubscriptionType type;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    @NotNull(message = "Le cycle de facturation est obligatoire")
    private BillingCycle billingCycle;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    @Min(value = 1, message = "Le nombre de projets doit être au moins 1")
    private Integer maxProjects;

    @Min(value = 1, message = "Le nombre de propositions doit être au moins 1")
    private Integer maxProposals;

    @Min(value = 1, message = "Le nombre de jobs actifs doit être au moins 1")
    private Integer maxActiveJobs;

    private Boolean featuredListing = false;
    private Boolean prioritySupport = false;
    private Boolean analyticsAccess = false;
}