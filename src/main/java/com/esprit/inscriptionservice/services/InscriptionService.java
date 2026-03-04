package com.esprit.inscriptionservice.services;

import com.esprit.inscriptionservice.clients.EventClient;
import com.esprit.inscriptionservice.clients.UserClient;
import com.esprit.inscriptionservice.dto.*;
import com.esprit.inscriptionservice.entities.EventInscription;
import com.esprit.inscriptionservice.entities.InscriptionStatus;
import com.esprit.inscriptionservice.repositories.InscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InscriptionService {

    private static final Logger log = LoggerFactory.getLogger(InscriptionService.class);

    private final InscriptionRepository inscriptionRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final BadgeGeneratorService badgeGeneratorService;
    private final EmailService emailService;

    public InscriptionService(InscriptionRepository inscriptionRepository,
                               EventClient eventClient,
                               UserClient userClient,
                               BadgeGeneratorService badgeGeneratorService,
                               EmailService emailService) {
        this.inscriptionRepository = inscriptionRepository;
        this.eventClient = eventClient;
        this.userClient = userClient;
        this.badgeGeneratorService = badgeGeneratorService;
        this.emailService = emailService;
    }

    @Transactional
    public InscriptionResponseDTO submitInscription(InscriptionRequestDTO request) {
        EventDTO event = eventClient.getEventById(request.getEventId());
        if (event == null) throw new RuntimeException("Événement introuvable: " + request.getEventId());

        UserDTO user = userClient.getUserById(Math.toIntExact(request.getUserId()));
        if (user == null) throw new RuntimeException("Utilisateur introuvable: " + request.getUserId());

        if (inscriptionRepository.existsByUserIdAndEventId(request.getUserId(), request.getEventId())) {
            throw new RuntimeException("Vous avez déjà soumis une demande pour cet événement");
        }

        if (event.getCapacity() != null && event.getCapacity() > 0) {
            long activeCount = inscriptionRepository.countByEventIdAndStatusNot(
                    request.getEventId(), InscriptionStatus.REJECTED);
            if (activeCount >= event.getCapacity()) {
                throw new RuntimeException("Maximum number of participants has been reached");
            }
        }

        EventInscription inscription = new EventInscription();
        inscription.setParticipantNom(request.getParticipantNom());
        inscription.setParticipantPrenom(request.getParticipantPrenom());
        inscription.setParticipantEmail(request.getParticipantEmail() != null
                ? request.getParticipantEmail() : user.getEmail());
        inscription.setDomaine(request.getDomaine());
        inscription.setParticipantRole(request.getParticipantRole());
        inscription.setImageUrl(request.getImageUrl());
        inscription.setMessage(request.getMessage());
        inscription.setRegistrationDate(LocalDateTime.now());
        inscription.setStatus(InscriptionStatus.PENDING);
        inscription.setUserId(request.getUserId());
        inscription.setEventId(request.getEventId());

        EventInscription saved = inscriptionRepository.save(inscription);
        return mapToResponse(saved, event, user);
    }

    @Transactional
    public InscriptionResponseDTO acceptInscription(Long inscriptionId) {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() != InscriptionStatus.PENDING)
            throw new RuntimeException("Cette demande a déjà été traitée");

        inscription.setStatus(InscriptionStatus.ACCEPTED);
        inscriptionRepository.save(inscription);

        EventDTO event = fetchEventSafely(inscription.getEventId());
        String badgePath = badgeGeneratorService.generateBadge(inscription, event);
        inscription.setBadgeImagePath(badgePath);
        inscriptionRepository.save(inscription);

        try {
            UserDTO user = fetchUserSafely(inscription.getUserId());
            String email = user != null ? user.getEmail() : inscription.getParticipantEmail();
            if (email != null) {
                emailService.sendAcceptanceEmail(email,
                        inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                        event != null ? event.getTitle() : "Événement", badgePath);
            }
        } catch (Exception e) {
            log.warn("Email d'acceptation non envoyé: {}", e.getMessage());
        }

        return mapToResponse(inscription, event, fetchUserSafely(inscription.getUserId()));
    }

    @Transactional
    public InscriptionResponseDTO rejectInscription(Long inscriptionId) {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() != InscriptionStatus.PENDING)
            throw new RuntimeException("Cette demande a déjà été traitée");

        inscription.setStatus(InscriptionStatus.REJECTED);
        inscriptionRepository.save(inscription);

        EventDTO event = fetchEventSafely(inscription.getEventId());
        try {
            UserDTO user = fetchUserSafely(inscription.getUserId());
            String email = user != null ? user.getEmail() : inscription.getParticipantEmail();
            if (email != null) {
                emailService.sendRejectionEmail(email,
                        inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                        event != null ? event.getTitle() : "Événement");
            }
        } catch (Exception e) {
            log.warn("Email de refus non envoyé: {}", e.getMessage());
        }

        return mapToResponse(inscription, event, fetchUserSafely(inscription.getUserId()));
    }

    public byte[] getBadgeBytes(Long inscriptionId) throws Exception {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        if (inscription.getStatus() != InscriptionStatus.ACCEPTED)
            throw new RuntimeException("Badge disponible uniquement pour les inscriptions acceptées");
        if (inscription.getBadgeImagePath() == null)
            throw new RuntimeException("Badge non généré");
        java.io.File file = new java.io.File(inscription.getBadgeImagePath());
        return java.nio.file.Files.readAllBytes(file.toPath());
    }

    public List<InscriptionResponseDTO> getAllInscriptionsByEvent(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        return inscriptionRepository.findByEventId(eventId).stream()
                .map(i -> mapToResponse(i, event, fetchUserSafely(i.getUserId())))
                .collect(Collectors.toList());
    }

    public List<InscriptionResponseDTO> getInscriptionsByUser(Long userId) {
        return inscriptionRepository.findByUserId(userId).stream()
                .map(i -> mapToResponse(i, fetchEventSafely(i.getEventId()), fetchUserSafely(i.getUserId())))
                .collect(Collectors.toList());
    }

    public List<InscriptionResponseDTO> getPendingInscriptions(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        return inscriptionRepository.findByEventIdAndStatus(eventId, InscriptionStatus.PENDING).stream()
                .map(i -> mapToResponse(i, event, fetchUserSafely(i.getUserId())))
                .collect(Collectors.toList());
    }

    public void deleteInscription(Long inscriptionId) {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        inscriptionRepository.delete(inscription);
    }

    public boolean isEventFull(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        if (event == null || event.getCapacity() == null || event.getCapacity() <= 0) return false;
        long activeCount = inscriptionRepository.countByEventIdAndStatusNot(eventId, InscriptionStatus.REJECTED);
        return activeCount >= event.getCapacity();
    }

    public Map<String, Object> getCapacityStatus(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        long activeCount = inscriptionRepository.countByEventIdAndStatusNot(eventId, InscriptionStatus.REJECTED);
        Map<String, Object> status = new HashMap<>();
        status.put("eventId", eventId);
        status.put("capacity", event != null ? event.getCapacity() : 0);
        status.put("activeParticipants", activeCount);
        status.put("isFull", event != null && event.getCapacity() != null
                && event.getCapacity() > 0 && activeCount >= event.getCapacity());
        return status;
    }

    private EventDTO fetchEventSafely(Long eventId) {
        if (eventId == null) return null;
        try { return eventClient.getEventById(eventId); } catch (Exception e) { return null; }
    }

    private UserDTO fetchUserSafely(Long userId) {
        if (userId == null) return null;
        try { return userClient.getUserById(Math.toIntExact(userId)); } catch (Exception e) { return null; }
    }

    private InscriptionResponseDTO mapToResponse(EventInscription i, EventDTO event, UserDTO user) {
        InscriptionResponseDTO dto = new InscriptionResponseDTO();
        dto.setId(i.getId());
        dto.setParticipantNom(i.getParticipantNom());
        dto.setParticipantPrenom(i.getParticipantPrenom());
        dto.setParticipantEmail(i.getParticipantEmail() != null ? i.getParticipantEmail()
                : (user != null ? user.getEmail() : null));
        dto.setRegistrationDate(i.getRegistrationDate());
        dto.setDomaine(i.getDomaine());
        dto.setParticipantRole(i.getParticipantRole());
        dto.setImageUrl(i.getImageUrl());
        dto.setMessage(i.getMessage());
        dto.setStatus(i.getStatus());
        dto.setBadgeImagePath(i.getBadgeImagePath());
        dto.setUserId(i.getUserId());
        dto.setEventId(i.getEventId());
        dto.setEventTitle(event != null ? event.getTitle() : null);
        dto.setEventStartDate(event != null ? event.getStartDate() : null);
        dto.setEventLocation(event != null ? event.getLocation() : null);
        return dto;
    }
}
