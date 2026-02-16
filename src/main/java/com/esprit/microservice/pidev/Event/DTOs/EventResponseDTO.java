package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private Long idEvent;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EventStatus eventStatus;
    private String location;
    private Integer capacity;
    private Integer currentParticipants;
    private String imageUrl;
    private CategoryEvent category;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime createdAt;
}
