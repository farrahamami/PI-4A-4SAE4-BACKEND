package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionResponseDTO;
import com.esprit.microservice.pidev.Event.Entities.*;
import com.esprit.microservice.pidev.Event.Repositories.InscriptionRepository;
import com.esprit.microservice.pidev.Event.Repositories.EventRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import com.esprit.microservice.pidev.Entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventInscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BadgeGeneratorService badgeGeneratorService;
    private final EmailService emailService;

    // ========================
    // USER : Soumettre une demande
    // ========================
    @Transactional
    public EventInscriptionResponseDTO submitInscription(EventInscriptionRequestDTO request) {

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));

        User user = userRepository.findById(Math.toIntExact(request.getUserId()))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (inscriptionRepository.existsByUserIdAndEventId(request.getUserId(), request.getEventId())) {
            throw new RuntimeException("Vous avez déjà soumis une demande pour cet événement");
        }

        // ✅ La vérification de capacité reste ici (côté user, on bloque les nouvelles demandes)
        if (event.getCapacity() > 0 && isEventFull(event)) {
            throw new RuntimeException("Maximum number of participants has been reached");
        }

        EventInscription inscription = new EventInscription();
        inscription.setParticipantNom(request.getParticipantNom());
        inscription.setParticipantPrenom(request.getParticipantPrenom());
        inscription.setDomaine(request.getDomaine());
        inscription.setParticipantrole(request.getParticipantRole());
        inscription.setImageUrl(request.getImageUrl());
        inscription.setMessage(request.getMessage());
        inscription.setRegistrationDate(LocalDateTime.now());
        inscription.setStatus(InscriptionStatus.PENDING);
        inscription.setUser(user);
        inscription.setEvent(event);

        EventInscription saved = inscriptionRepository.save(inscription);
        return mapToResponse(saved);
    }

    // ========================
    // ADMIN : Accepter une demande
    // ========================
    @Transactional
    public EventInscriptionResponseDTO acceptInscription(Long inscriptionId) {

        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() != InscriptionStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        // ✅ FIX 1 : Suppression du check de capacité ici.
        // L'admin décide librement d'accepter ou refuser, peu importe la capacité.

        inscription.setStatus(InscriptionStatus.ACCEPTED);
        inscriptionRepository.save(inscription);

        // Générer le badge
        String badgePath = badgeGeneratorService.generateBadge(inscription);
        inscription.setBadgeImagePath(badgePath);
        inscriptionRepository.save(inscription);

        // ✅ FIX 2 : L'email ne bloque plus le process si la config mail est mauvaise
        try {
            String userEmail = inscription.getUser().getEmail();
            emailService.sendAcceptanceEmail(
                    userEmail,
                    inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                    inscription.getEvent().getTitle(),
                    badgePath
            );
        } catch (Exception e) {
            log.warn("Email d'acceptation non envoyé (config mail?) : {}", e.getMessage());
        }

        return mapToResponse(inscription);
    }

    // ========================
    // ADMIN : Refuser une demande
    // ========================
    @Transactional
    public EventInscriptionResponseDTO rejectInscription(Long inscriptionId) {

        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() != InscriptionStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        inscription.setStatus(InscriptionStatus.REJECTED);
        inscriptionRepository.save(inscription);

        // ✅ FIX 2 : L'email ne bloque plus le process si la config mail est mauvaise
        try {
            String userEmail = inscription.getUser().getEmail();
            emailService.sendRejectionEmail(
                    userEmail,
                    inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                    inscription.getEvent().getTitle()
            );
        } catch (Exception e) {
            log.warn("Email de refus non envoyé (config mail?) : {}", e.getMessage());
        }

        return mapToResponse(inscription);
    }

    // ========================
    // ADMIN : Récupérer le badge
    // ========================
    public byte[] getBadgeBytes(Long inscriptionId) throws Exception {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() != InscriptionStatus.ACCEPTED) {
            throw new RuntimeException("Badge disponible uniquement pour les inscriptions acceptées");
        }

        if (inscription.getBadgeImagePath() == null) {
            throw new RuntimeException("Badge non généré");
        }

        java.io.File file = new java.io.File(inscription.getBadgeImagePath());
        return java.nio.file.Files.readAllBytes(file.toPath());
    }

    // ========================
    // Getters divers
    // ========================
    public List<EventInscriptionResponseDTO> getAllInscriptionsByEvent(Long eventId) {
        return inscriptionRepository.findByEventId(eventId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<EventInscriptionResponseDTO> getInscriptionsByUser(Long userId) {
        return inscriptionRepository.findByUserId(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<EventInscriptionResponseDTO> getPendingInscriptions(Long eventId) {
        return inscriptionRepository.findByEventIdAndStatus(eventId, InscriptionStatus.PENDING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public void deleteInscription(Long inscriptionId) {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        inscriptionRepository.delete(inscription);
    }

    // ========================
    // Vérification capacité
    // ========================
    public boolean isEventFull(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));
        return isEventFull(event);
    }

    public java.util.Map<String, Object> getCapacityStatus(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));
        long activeCount = inscriptionRepository
                .countByEventIdAndStatusNot(eventId, InscriptionStatus.REJECTED);
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("eventId", eventId);
        status.put("capacity", event.getCapacity());
        status.put("activeParticipants", activeCount);
        status.put("isFull", event.getCapacity() > 0 && activeCount >= event.getCapacity());
        return status;
    }

    private boolean isEventFull(Event event) {
        long activeCount = inscriptionRepository
                .countByEventIdAndStatusNot(event.getIdEvent(), InscriptionStatus.REJECTED);
        return activeCount >= event.getCapacity();
    }

    // ========================
    // Mapper
    // ========================
    private EventInscriptionResponseDTO mapToResponse(EventInscription i) {
        return EventInscriptionResponseDTO.builder()
                .id(i.getId())
                .participantNom(i.getParticipantNom())
                .participantPrenom(i.getParticipantPrenom())
                .participantEmail(i.getUser() != null ? i.getUser().getEmail() : null)  // ✅ CORRECT
                .registrationDate(i.getRegistrationDate())
                .domaine(i.getDomaine())
                .participantRole(i.getParticipantrole())
                .imageUrl(i.getImageUrl())
                .message(i.getMessage())
                .status(i.getStatus())
                .badgeImagePath(i.getBadgeImagePath())
                .userId(i.getUser() != null ? (long) i.getUser().getId() : null)
                .eventId(i.getEvent() != null ? i.getEvent().getIdEvent() : null)
                .eventTitle(i.getEvent() != null ? i.getEvent().getTitle() : null)
                .eventStartDate(i.getEvent().getStartDate())
                .eventLocation(i.getEvent().getLocation())
                .build();
    }
}