package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.Domaine;
import com.esprit.microservice.pidev.Event.Entities.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder                  // ✅ C'est ça qui génère la méthode builder()
@NoArgsConstructor
@AllArgsConstructor
public class EventInscriptionRequestDTO {

    private String participantNom;
    private String participantPrenom;
    private Domaine demaine;
    private ParticipantRole participantRole;
    private String message;
    private Long userId;
    private Long eventId;

    public String getParticipantNom() {
        return participantNom;
    }

    public void setParticipantNom(String participantNom) {
        this.participantNom = participantNom;
    }

    public String getParticipantPrenom() {
        return participantPrenom;
    }

    public void setParticipantPrenom(String participantPrenom) {
        this.participantPrenom = participantPrenom;
    }

    public Domaine getDemaine() {
        return demaine;
    }

    public void setDemaine(Domaine domaine) {
        this.demaine = domaine;
    }

    public ParticipantRole getParticipantRole() {
        return participantRole;
    }

    public void setParticipantRole(ParticipantRole participantRole) {
        this.participantRole = participantRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
