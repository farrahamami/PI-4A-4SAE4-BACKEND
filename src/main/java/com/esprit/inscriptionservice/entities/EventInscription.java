package com.esprit.inscriptionservice.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_inscriptions")
public class EventInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String participantNom;
    private String participantPrenom;
    private String participantEmail;
    private LocalDateTime registrationDate;
    private String badgeImagePath;

    @Column(columnDefinition = "LONGTEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private Domaine domaine;

    @Enumerated(EnumType.STRING)
    private ParticipantRole participantRole;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private InscriptionStatus status;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_id")
    private Long eventId;

    public EventInscription() {}

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
    public String getBadgeImagePath() { return badgeImagePath; }
    public void setBadgeImagePath(String badgeImagePath) { this.badgeImagePath = badgeImagePath; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Domaine getDomaine() { return domaine; }
    public void setDomaine(Domaine domaine) { this.domaine = domaine; }
    public ParticipantRole getParticipantRole() { return participantRole; }
    public void setParticipantRole(ParticipantRole participantRole) { this.participantRole = participantRole; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public InscriptionStatus getStatus() { return status; }
    public void setStatus(InscriptionStatus status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}
