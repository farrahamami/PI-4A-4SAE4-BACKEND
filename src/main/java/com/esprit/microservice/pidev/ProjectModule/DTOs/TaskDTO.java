package com.esprit.microservice.pidev.ProjectModule.DTOs;

import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private String taskName;
    private String description;
    private Priority priority;
    private String milestone;
    private LocalDate startDate;
    private LocalDate endDate;
}