package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.Entities.EventInscription;
import com.esprit.microservice.pidev.Event.Entities.InscriptionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IInscriptionService {

    EventInscription createInscription(EventInscription inscription,
                                       Long userId,
                                       Long eventId,
                                       MultipartFile photo);

    EventInscription acceptInscription(Long inscriptionId);

    EventInscription refuseInscription(Long inscriptionId);

    byte[] getBadgeBytes(Long inscriptionId) throws IOException;

    List<EventInscription> getInscriptionsByEvent(Long eventId);

    List<EventInscription> getInscriptionsByEventAndStatus(Long eventId, InscriptionStatus status);

    List<EventInscription> getInscriptionsByUser(Long userId);

    List<EventInscription> getAllInscriptions();

    EventInscription getInscriptionById(Long inscriptionId);

    void deleteInscription(Long inscriptionId);
}
