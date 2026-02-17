package com.esprit.microservice.pidev.ProjectModule.Domain.Entities;

import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.Priority;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String taskName;

    @Column(length = 1000)
    String description;

    LocalDate startDate;
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    Priority priority;

    String milestone;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "project_id")
    Project project;
}