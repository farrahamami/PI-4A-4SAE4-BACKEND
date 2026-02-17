package com.esprit.microservice.pidev.ProjectModule.DTOs;

import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.Category;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequestDTO {
    private String title;
    private String description;
    private Double budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private Category category;
    private Integer clientId;           // just the client ID
    private List<Integer> skillIds;
    private List<TaskDTO> tasks;
}
