package com.esprit.microservice.pidev.ProjectModule.Domain.Entities;
import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.Category;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

   String title;

    String description;

    Double budget;

   LocalDate startDate;
   LocalDate endDate;

    @Enumerated(EnumType.STRING)
    ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    Category category;

    LocalDate createdAt;

    // Relation avec User (Client)
    @ManyToOne
    @JoinColumn(name = "client_id")
    User client;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    List<Task> tasks;

    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "project_skills",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<FreelancerSkill> requiredSkills;

}
