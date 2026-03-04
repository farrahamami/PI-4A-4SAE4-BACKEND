package com.esprit.inscriptionservice.dto;

import com.esprit.inscriptionservice.entities.Domaine;
import com.esprit.inscriptionservice.entities.InscriptionStatus;
import com.esprit.inscriptionservice.entities.ParticipantRole;
import java.time.LocalDateTime;

public class InscriptionResponseDTO {

    private Long id;
    private String participantNom;
    private String participantPrenom;
    private String participantEmail;
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
    private LocalDateTime eventStartDate;
    private String eventLocation;

    public InscriptionResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getParticipantNom() { return participantNom; }
    public void setParticipantNom(String participantNom) { this.participantNom = participantNom; }

    public String getParticipantPrenom() { return participantPrenom; }
    public void setParticipantPrenom(String participantPrenom) { this.participantPrenom = participantPrenom; }

    public String getParticipantEmail() { return participantEmail; }
    public void setParticipantEmail(String participantEmail) { this.participantEmail = participantEmail; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public Domaine getDomaine() { return domaine; }
    public void setDomaine(Domaine domaine) { this.domaine = domaine; }

    public ParticipantRole getParticipantRole() { return participantRole; }
    public void setParticipantRole(ParticipantRole participantRole) { this.participantRole = participantRole; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getBadgeImagePath() { return badgeImagePath; }
    public void setBadgeImagePath(String badgeImagePath) { this.badgeImagePath = badgeImagePath; }

    public InscriptionStatus getStatus() { return status; }
    public void setStatus(InscriptionStatus status) { this.status = status; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public LocalDateTime getEventStartDate() { return eventStartDate; }
    public void setEventStartDate(LocalDateTime eventStartDate) { this.eventStartDate = eventStartDate; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }
}
