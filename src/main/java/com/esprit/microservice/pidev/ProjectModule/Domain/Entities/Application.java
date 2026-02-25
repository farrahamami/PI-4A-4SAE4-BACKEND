package com.esprit.microservice.pidev.ProjectModule.Domain.Entities;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne
    @JoinColumn(name = "freelancer_id")
    User freelancer;

    @ManyToOne
    @JoinColumn(name = "project_id")
    Project project;

    String coverLetterUrl;   // PDF uploadé ou généré

    LocalDate appliedAt;

    @Enumerated(EnumType.STRING)
    ApplicationStatus status; // PENDING, ACCEPTED, REJECTED
}