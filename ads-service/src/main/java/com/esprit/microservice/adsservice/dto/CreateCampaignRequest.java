package com.esprit.microservice.adsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCampaignRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String imageUrl;

    private String targetUrl;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private String roleType;

    private Long targetId;
}
