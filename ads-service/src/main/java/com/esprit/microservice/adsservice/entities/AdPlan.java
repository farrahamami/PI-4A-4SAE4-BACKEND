package com.esprit.microservice.adsservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdType type;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleType;

    private String description;

    private String icon;

    private Integer durationDays;
}
