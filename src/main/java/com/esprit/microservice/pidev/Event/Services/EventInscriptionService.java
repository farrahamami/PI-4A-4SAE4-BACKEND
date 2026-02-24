package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionResponseDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionResponseDTO;
import com.esprit.microservice.pidev.Event.Entities.*;
import com.esprit.microservice.pidev.Event.Repositories.InscriptionRepository;
import com.esprit.microservice.pidev.Event.Repositories.EventRepository;
import com.esprit.microservice.pidev.Event.Repositories.InscriptionRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import com.esprit.microservice.pidev.Entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // 1. Vérifier que l'événement existe
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));

        // 2. Vérifier que l'utilisateur existe
        User user = userRepository.findById(Math.toIntExact(request.getUserId()))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 3. Vérifier si l'utilisateur est déjà inscrit
        if (inscriptionRepository.existsByUserIdAndEventId(request.getUserId(), request.getEventId())) {
            throw new RuntimeException("Vous avez déjà soumis une demande pour cet événement");
        }

        // 4. Vérifier la capacité maximale (places acceptées)
        long acceptedCount = inscriptionRepository
                .countByEventIdAndStatus(request.getEventId(), InscriptionStatus.ACCEPTED);

        if (event.getCurrentParticipants() > 0 && acceptedCount >= event.getCurrentParticipants()) {
            throw new RuntimeException("L'événement a atteint sa capacité maximale de participants");
        }

        // 5. Créer l'inscription
        EventInscription inscription = new EventInscription();
        inscription.setParticipantNom(request.getParticipantNom());
        inscription.setParticipantPrenom(request.getParticipantPrenom());
        inscription.setDemaine(request.getDemaine());
        inscription.setParticipantrole(request.getParticipantRole());
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

        // Vérifier à nouveau la capacité au moment de l'acceptation
        Event event = inscription.getEvent();
        long acceptedCount = inscriptionRepository
                .countByEventIdAndStatus(event.getIdEvent(), InscriptionStatus.ACCEPTED);

        if (event.getCurrentParticipants() > 0 && acceptedCount >= event.getCurrentParticipants()) {
            throw new RuntimeException("L'événement a atteint sa capacité maximale de participants");
        }

        // Changer le statut
        inscription.setStatus(InscriptionStatus.ACCEPTED);
        inscriptionRepository.save(inscription);

        // Générer le badge
        String badgePath = badgeGeneratorService.generateBadge(inscription);
        inscription.setBadgeImagePath(badgePath);
        inscriptionRepository.save(inscription);

        // Envoyer l'email avec badge en pièce jointe
        String userEmail = inscription.getUser().getEmail();
        emailService.sendAcceptanceEmail(
                userEmail,
                inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                event.getTitle(),
                badgePath
        );

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

        // Envoyer email de refus
        String userEmail = inscription.getUser().getEmail();
        emailService.sendRejectionEmail(
                userEmail,
                inscription.getParticipantPrenom() + " " + inscription.getParticipantNom(),
                inscription.getEvent().getTitle()
        );

        return mapToResponse(inscription);
    }

    // ========================
    // ADMIN : Imprimer/récupérer le badge
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
    // Mapper
    // ========================
    private EventInscriptionResponseDTO mapToResponse(EventInscription i) {
        return EventInscriptionResponseDTO.builder()
                .id(i.getId())
                .participantNom(i.getParticipantNom())
                .participantPrenom(i.getParticipantPrenom())
                .registrationDate(i.getRegistrationDate())
                .domaine(i.getDemaine())
                .participantRole(i.getParticipantrole())
                .imageUrl(i.getImageUrl())
                .status(i.getStatus())
                .badgeImagePath(i.getBadgeImagePath())
                .userId(i.getUser() != null ? (long) i.getUser().getId() : null)
                .eventId(i.getEvent() != null ? i.getEvent().getIdEvent() : null)
                .eventTitle(i.getEvent() != null ? i.getEvent().getTitle() : null)
                .build();
    }
}
