package com.esprit.microservice.pidev.modules.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long userId;

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private Long subscriptionId;

    private Boolean autoRenew = true;

    private String paymentMethod;
    private String transactionId;
}