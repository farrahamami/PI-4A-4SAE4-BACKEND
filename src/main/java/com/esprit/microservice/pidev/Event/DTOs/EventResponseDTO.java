package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO  {

    private Long idEvent;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EventStatus eventStatus;
    private String location;
    private Integer capacity;
    private Integer currentParticipants;
    private Long activeParticipantsCount;  // PENDING + ACCEPTED (exclut REJECTED)
    private Boolean isFull;                // true quand activeParticipantsCount >= capacity
    private String imageUrl;
    private CategoryEvent category;
    private Integer userId;
    private String userName;
    private LocalDateTime createdAt;
    private Double latitude;
    private Double longitude;
    private boolean archived;
    private List<ActivityRequestDTO> activities;



}