package com.esprit.microservice.pidev.Event.DTOs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequestDTO {
    private Long idActivity;
    private String name;
    private String description;
    private String requirements;
    private Integer maxParticipants;
}