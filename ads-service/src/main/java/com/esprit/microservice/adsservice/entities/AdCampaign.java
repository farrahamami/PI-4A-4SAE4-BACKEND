package com.esprit.microservice.adsservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String imageUrl;

    private String targetUrl;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private AdPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdStatus status;

    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    private Long targetId;

    private LocalDateTime createdAt;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long views;

    private Long clicks;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.views == null) this.views = 0L;
        if (this.clicks == null) this.clicks = 0L;
    }
}
