package com.esprit.microservice.pidev.Event.Entities;

import com.esprit.microservice.pidev.Entities.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

public class EventInscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;
    String ParticipantNom;
    String ParticipantPrenom;
    String ParticipantEmail;
    LocalDateTime registrationDate;
    String badgeImagePath;
    @Column(columnDefinition = "LONGTEXT", nullable = true)
    String message;

    @Enumerated(EnumType.STRING)
    Domaine domaine;

    @Enumerated(EnumType.STRING)
    ParticipantRole participantrole;

    @Column(columnDefinition = "LONGTEXT", nullable = true)
    String imageUrl;


    @Enumerated(EnumType.STRING)
    InscriptionStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    User user;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @ToString.Exclude
    Event event;

    public EventInscription(Long id, String participantNom, String participantPrenom, LocalDateTime registrationDate, String badgeImagePath, Domaine demaine, ParticipantRole participantrole, String imageUrl, InscriptionStatus status, User user, Event event) {
        this.id = id;
        ParticipantNom = participantNom;
        ParticipantPrenom = participantPrenom;
        this.registrationDate = registrationDate;
        this.badgeImagePath = badgeImagePath;
        this.domaine = domaine;
        this.participantrole = participantrole;
        this.imageUrl = imageUrl;
        this.status = status;
        this.user = user;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParticipantNom() {
        return ParticipantNom;
    }

    public void setParticipantNom(String participantNom) {
        ParticipantNom = participantNom;
    }

    public String getParticipantPrenom() {
        return ParticipantPrenom;
    }

    public void setParticipantPrenom(String participantPrenom) {
        ParticipantPrenom = participantPrenom;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getBadgeImagePath() {
        return badgeImagePath;
    }

    public void setBadgeImagePath(String badgeImagePath) {
        this.badgeImagePath = badgeImagePath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Domaine getDomaine() {
        return domaine;
    }

    public void setDomaine(Domaine demaine) {
        this.domaine = demaine;
    }

    public ParticipantRole getParticipantrole() {
        return participantrole;
    }

    public void setParticipantrole(ParticipantRole participantrole) {
        this.participantrole = participantrole;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public InscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(InscriptionStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getParticipantEmail() {
        return ParticipantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        ParticipantEmail = participantEmail;
    }
}
