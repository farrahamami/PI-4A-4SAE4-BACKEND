package com.esprit.microservice.adsservice.services;

import com.esprit.microservice.adsservice.dto.CreateCampaignRequest;
import com.esprit.microservice.adsservice.entities.*;
import com.esprit.microservice.adsservice.exception.BadRequestException;
import com.esprit.microservice.adsservice.exception.ResourceNotFoundException;
import com.esprit.microservice.adsservice.repositories.AdCampaignRepository;
import com.esprit.microservice.adsservice.repositories.AdPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdCampaignService {

    private final AdCampaignRepository campaignRepository;
    private final AdPlanRepository planRepository;

    @Transactional
    public AdCampaign createCampaign(CreateCampaignRequest request, Long userId) {
        log.info("Processing request for User ID: {} and Plan ID: {}", userId, request.getPlanId());
        
        AdPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Ad plan not found with id: " + request.getPlanId()));

        RoleType roleType = plan.getRoleType();
        if (request.getRoleType() != null && !request.getRoleType().isBlank()) {
            try {
                roleType = RoleType.valueOf(request.getRoleType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int durationDays = plan.getDurationDays() != null ? plan.getDurationDays() : 30;

        AdCampaign campaign = AdCampaign.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .targetUrl(request.getTargetUrl())
                .userId(userId)
                .plan(plan)
                .status(AdStatus.PENDING)
                .roleType(roleType)
                .targetId(request.getTargetId())
                .startDate(now)
                .endDate(now.plusDays(durationDays))
                .views(0L)
                .clicks(0L)
                .build();

        try {
            AdCampaign savedCampaign = campaignRepository.save(campaign);
            log.info("Campaign created successfully with ID: {}", savedCampaign.getId());
            return savedCampaign;
        } catch (DataIntegrityViolationException e) {
            log.error("Database integrity error while saving campaign: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to save campaign due to data integrity violation: " + e.getMostSpecificCause().getMessage());
        }
    }

    public List<AdCampaign> getMyCampaigns(Long userId) {
        return campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<AdCampaign> getMyCampaignsByRole(Long userId, RoleType roleType) {
        return campaignRepository.findByUserIdAndRoleTypeOrderByCreatedAtDesc(userId, roleType);
    }

    public List<AdCampaign> getActiveCampaigns() {
        return campaignRepository.findByStatusOrderByCreatedAtDesc(AdStatus.ACTIVE);
    }

    public List<AdCampaign> getAllCampaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public AdCampaign approveCampaign(Long id) {
        AdCampaign campaign = findById(id);
        if (campaign.getStatus() != AdStatus.PENDING) {
            throw new BadRequestException("Only PENDING campaigns can be approved. Current status: " + campaign.getStatus());
        }
        campaign.setStatus(AdStatus.ACTIVE);
        campaign.setRejectionReason(null);
        LocalDateTime now = LocalDateTime.now();
        int durationDays = campaign.getPlan().getDurationDays() != null ? campaign.getPlan().getDurationDays() : 30;
        campaign.setStartDate(now);
        campaign.setEndDate(now.plusDays(durationDays));
        return campaignRepository.save(campaign);
    }

    @Transactional
    public AdCampaign rejectCampaign(Long id, String reason) {
        AdCampaign campaign = findById(id);
        if (campaign.getStatus() != AdStatus.PENDING) {
            throw new BadRequestException("Only PENDING campaigns can be rejected. Current status: " + campaign.getStatus());
        }
        campaign.setStatus(AdStatus.REJECTED);
        campaign.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Rejected by admin.");
        return campaignRepository.save(campaign);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        AdCampaign campaign = findById(id);
        campaignRepository.delete(campaign);
    }

    @Transactional
    public AdCampaign updateCampaign(Long id, CreateCampaignRequest request, Long userId) {
        AdCampaign campaign = findById(id);
        if (!campaign.getUserId().equals(userId)) {
            throw new BadRequestException("You can only update your own campaigns.");
        }
        if (campaign.getStatus() != AdStatus.PENDING && campaign.getStatus() != AdStatus.REJECTED) {
            throw new BadRequestException("Only PENDING or REJECTED campaigns can be edited.");
        }

        AdPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Ad plan not found with id: " + request.getPlanId()));

        campaign.setTitle(request.getTitle());
        campaign.setDescription(request.getDescription());
        campaign.setImageUrl(request.getImageUrl());
        campaign.setTargetUrl(request.getTargetUrl());
        campaign.setPlan(plan);
        campaign.setStatus(AdStatus.PENDING);
        campaign.setRejectionReason(null);

        if (request.getRoleType() != null && !request.getRoleType().isBlank()) {
            try {
                campaign.setRoleType(RoleType.valueOf(request.getRoleType().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        campaign.setTargetId(request.getTargetId());

        return campaignRepository.save(campaign);
    }

    private AdCampaign findById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }
}
