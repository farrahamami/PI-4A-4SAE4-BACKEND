package com.esprit.microservice.pidev.modules.subscription.mapper;


import com.esprit.microservice.pidev.modules.subscription.domain.entities.Subscription;
import com.esprit.microservice.pidev.modules.subscription.dto.request.CreateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.request.UpdateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.SubscriptionResponse;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .name(subscription.getName())
                .type(subscription.getType())
                .price(subscription.getPrice())
                .billingCycle(subscription.getBillingCycle())
                .description(subscription.getDescription())
                .maxProjects(subscription.getMaxProjects())
                .maxProposals(subscription.getMaxProposals())
                .maxActiveJobs(subscription.getMaxActiveJobs())
                .featuredListing(subscription.getFeaturedListing())
                .prioritySupport(subscription.getPrioritySupport())
                .analyticsAccess(subscription.getAnalyticsAccess())
                .isActive(subscription.getIsActive())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public Subscription toEntity(CreateSubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setName(request.getName());
        subscription.setType(request.getType());
        subscription.setPrice(request.getPrice());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setDescription(request.getDescription());
        subscription.setMaxProjects(request.getMaxProjects());
        subscription.setMaxProposals(request.getMaxProposals());
        subscription.setMaxActiveJobs(request.getMaxActiveJobs());
        subscription.setFeaturedListing(request.getFeaturedListing());
        subscription.setPrioritySupport(request.getPrioritySupport());
        subscription.setAnalyticsAccess(request.getAnalyticsAccess());
        subscription.setIsActive(true);
        return subscription;
    }

    public void updateEntity(Subscription subscription, UpdateSubscriptionRequest request) {
        if (request.getName() != null) {
            subscription.setName(request.getName());
        }
        if (request.getPrice() != null) {
            subscription.setPrice(request.getPrice());
        }
        if (request.getBillingCycle() != null) {
            subscription.setBillingCycle(request.getBillingCycle());
        }
        if (request.getDescription() != null) {
            subscription.setDescription(request.getDescription());
        }
        if (request.getMaxProjects() != null) {
            subscription.setMaxProjects(request.getMaxProjects());
        }
        if (request.getMaxProposals() != null) {
            subscription.setMaxProposals(request.getMaxProposals());
        }
        if (request.getMaxActiveJobs() != null) {
            subscription.setMaxActiveJobs(request.getMaxActiveJobs());
        }
        if (request.getFeaturedListing() != null) {
            subscription.setFeaturedListing(request.getFeaturedListing());
        }
        if (request.getPrioritySupport() != null) {
            subscription.setPrioritySupport(request.getPrioritySupport());
        }
        if (request.getAnalyticsAccess() != null) {
            subscription.setAnalyticsAccess(request.getAnalyticsAccess());
        }
        if (request.getIsActive() != null) {
            subscription.setIsActive(request.getIsActive());
        }
    }
}