package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endDate;

    @NotBlank(message = "Le lieu est obligatoire")
    private String location;

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité doit être au moins 1")
    private Integer capacity;

    private String imageUrl;

    @NotNull(message = "La catégorie est obligatoire")
    private CategoryEvent category;

    @NotNull(message = "L'organisateur est obligatoire")
    private Integer userId;

    private List<ActivityRequestDTO> activities;



}
