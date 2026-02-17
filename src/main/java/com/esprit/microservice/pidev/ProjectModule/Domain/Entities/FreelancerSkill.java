package com.esprit.microservice.pidev.ProjectModule.Domain.Entities;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.Availability;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.SkillLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "freelancer_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreelancerSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String skillName;


    String description;

    @Enumerated(EnumType.STRING)
    SkillLevel level;

    Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    Availability availability;

    String resumeUrl;

    LocalDate createdAt;

    // Relation avec User (Freelancer)
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    @JsonIgnore
    @ManyToMany(mappedBy = "requiredSkills")
    private List<Project> projects;
}
