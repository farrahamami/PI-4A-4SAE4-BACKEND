package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.Domaine;
import com.esprit.microservice.pidev.Event.Entities.InscriptionStatus;
import com.esprit.microservice.pidev.Event.Entities.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInscriptionResponseDTO {

    private Long id;
    private String participantNom;
    private String participantPrenom;
    private LocalDateTime registrationDate;
    private Domaine domaine;
    private ParticipantRole participantRole;
    private String imageUrl;
    private String message;
    private String badgeImagePath;
    private InscriptionStatus status;
    private Long userId;
    private Long eventId;
    private String eventTitle;
}