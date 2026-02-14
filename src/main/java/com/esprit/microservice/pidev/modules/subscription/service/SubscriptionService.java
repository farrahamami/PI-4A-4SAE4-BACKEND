package com.esprit.microservice.pidev.modules.subscription.service;


import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
import com.esprit.microservice.pidev.modules.subscription.dto.request.CreateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.request.UpdateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);

    List<SubscriptionResponse> getAllSubscriptions();

    List<SubscriptionResponse> getActiveSubscriptions();

    List<SubscriptionResponse> getSubscriptionsByType(SubscriptionType type);

    SubscriptionResponse getSubscriptionById(Long id);

    SubscriptionResponse updateSubscription(Long id, UpdateSubscriptionRequest request);

    void deactivateSubscription(Long id);

    void activateSubscription(Long id);

    void deleteSubscription(Long id);
}