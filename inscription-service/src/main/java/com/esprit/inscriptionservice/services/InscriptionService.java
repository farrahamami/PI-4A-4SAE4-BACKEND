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

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns true if we are strictly before J-1 (i.e. event starts more than
     * 1 full day from now). All lock-sensitive operations must call this.
     */
    private boolean isBeforeDeadline(EventDTO event) {
        if (event == null || event.getStartDate() == null) return true; // safe default
        return LocalDateTime.now().isBefore(event.getStartDate().minusDays(1));
    }

    private long confirmedCount(Long eventId) {
        // Counts every status that "holds" a seat: ACCEPTED, PROMOTED
        return inscriptionRepository.countByEventIdAndStatus(eventId, InscriptionStatus.ACCEPTED)
                + inscriptionRepository.countByEventIdAndStatus(eventId, InscriptionStatus.PROMOTED);
    }

    private boolean isFull(EventDTO event, Long eventId) {
        if (event == null || event.getCapacity() == null || event.getCapacity() <= 0) return false;
        return confirmedCount(eventId) >= event.getCapacity();
    }

    // ─── Submit ─────────────────────────────────────────────────────────────────

    @Transactional
    public InscriptionResponseDTO submitInscription(InscriptionRequestDTO request) {
        EventDTO event = eventClient.getEventById(request.getEventId());
        if (event == null) throw new RuntimeException("Événement introuvable: " + request.getEventId());

        UserDTO user = userClient.getUserById(Math.toIntExact(request.getUserId()));
        if (user == null) throw new RuntimeException("Utilisateur introuvable: " + request.getUserId());

        // J-1 gate: inscriptions closed
        if (!isBeforeDeadline(event)) {
            throw new RuntimeException("Les inscriptions sont fermées pour cet événement");
        }

        if (inscriptionRepository.existsByUserIdAndEventId(request.getUserId(), request.getEventId())) {
            throw new RuntimeException("Vous avez déjà soumis une demande pour cet événement");
        }

        EventInscription inscription = buildInscription(request, user);

        if (isFull(event, request.getEventId())) {
            // Place on waitlist instead
            inscription.setStatus(InscriptionStatus.WAITLIST);
            inscription.setWaitlistDate(LocalDateTime.now());
            EventInscription saved = inscriptionRepository.save(inscription);

            return mapToResponse(saved, event, user);
        }

        inscription.setStatus(InscriptionStatus.PENDING);
        EventInscription saved = inscriptionRepository.save(inscription);
        return mapToResponse(saved, event, user);
    }

    // ─── Accept / Reject (admin) ─────────────────────────────────────────────

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
            String email = resolveEmail(inscription, user);
            if (email != null) {
                emailService.sendAcceptanceEmail(email,
                        fullName(inscription),
                        eventTitle(event), badgePath);
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
            String email = resolveEmail(inscription, user);
            if (email != null) {
                emailService.sendRejectionEmail(email, fullName(inscription), eventTitle(event));
            }
        } catch (Exception e) {
            log.warn("Email de refus non envoyé: {}", e.getMessage());
        }

        return mapToResponse(inscription, event, fetchUserSafely(inscription.getUserId()));
    }

    // ─── Cancel (participant) ────────────────────────────────────────────────

    @Transactional
    public InscriptionResponseDTO cancelInscription(Long inscriptionId) {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        if (inscription.getStatus() == InscriptionStatus.CANCELLED)
            throw new RuntimeException("Cette inscription est déjà annulée");
        if (inscription.getStatus() == InscriptionStatus.REJECTED)
            throw new RuntimeException("Une inscription rejetée ne peut pas être annulée");

        EventDTO event = fetchEventSafely(inscription.getEventId());

        // J-1 gate: cancellations locked
        if (!isBeforeDeadline(event)) {
            throw new RuntimeException("Les annulations ne sont plus possibles à moins de 24h de l'événement");
        }

        boolean wasConfirmed = inscription.getStatus() == InscriptionStatus.ACCEPTED
                || inscription.getStatus() == InscriptionStatus.PROMOTED;

        inscription.setStatus(InscriptionStatus.CANCELLED);
        inscriptionRepository.save(inscription);

        // Trigger FIFO promotion only when a confirmed seat is freed
        if (wasConfirmed) {
            promoteFromWaitlist(inscription.getEventId(), event, 1);
        }

        return mapToResponse(inscription, event, fetchUserSafely(inscription.getUserId()));
    }

    // ─── Capacity increase (admin) ───────────────────────────────────────────

    /**
     * Called by an admin endpoint when the event capacity is increased.
     * newCapacity is the NEW total capacity (already saved in event-service).
     * We compute how many extra seats are now available and promote that many
     * waitlisted participants (FIFO), but only if we are before J-1.
     */
    @Transactional
    public void handleCapacityIncrease(Long eventId, int newCapacity) {
        EventDTO event = fetchEventSafely(eventId);
        long currentConfirmed = confirmedCount(eventId);
        long availableSeats = newCapacity - currentConfirmed;

        if (availableSeats <= 0) return; // no new slots

        if (!isBeforeDeadline(event)) {
            log.info("Capacity increased for event {} but J-1 passed — no promotions", eventId);
            return;
        }

        promoteFromWaitlist(eventId, event, (int) availableSeats);
    }

    // ─── FIFO promotion core ─────────────────────────────────────────────────

    /**
     * Promotes up to `count` participants from the WAITLIST (FIFO by waitlistDate).
     * Generates their badge and sends acceptance email.
     * Must be called only when isBeforeDeadline has already been checked.
     */
    private void promoteFromWaitlist(Long eventId, EventDTO event, int count) {
        List<EventInscription> waitlist =
                inscriptionRepository.findWaitlistByEventIdOrderedFIFO(eventId);

        int promoted = 0;
        for (EventInscription candidate : waitlist) {
            if (promoted >= count) break;

            candidate.setStatus(InscriptionStatus.PROMOTED);
            inscriptionRepository.save(candidate);

            // Generate badge
            try {
                String badgePath = badgeGeneratorService.generateBadge(candidate, event);
                candidate.setBadgeImagePath(badgePath);
                inscriptionRepository.save(candidate);
            } catch (Exception e) {
                log.warn("Badge non généré pour promotion {}: {}", candidate.getId(), e.getMessage());
            }

            // Send promotion email


            promoted++;
        }

        log.info("Promoted {} participant(s) from waitlist for event {}", promoted, eventId);
    }

    // ─── Read operations ─────────────────────────────────────────────────────

    public byte[] getBadgeBytes(Long inscriptionId) throws Exception {
        EventInscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        if (inscription.getStatus() != InscriptionStatus.ACCEPTED
                && inscription.getStatus() != InscriptionStatus.PROMOTED)
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

    public List<InscriptionResponseDTO> getWaitlistByEvent(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        return inscriptionRepository.findWaitlistByEventIdOrderedFIFO(eventId).stream()
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
        return isFull(event, eventId);
    }

    public Map<String, Object> getCapacityStatus(Long eventId) {
        EventDTO event = fetchEventSafely(eventId);
        long confirmed = confirmedCount(eventId);
        long waitlistSize = inscriptionRepository.countByEventIdAndStatus(eventId, InscriptionStatus.WAITLIST);
        Map<String, Object> status = new HashMap<>();
        status.put("eventId", eventId);
        status.put("capacity", event != null ? event.getCapacity() : 0);
        status.put("confirmedParticipants", confirmed);
        status.put("waitlistSize", waitlistSize);
        status.put("isFull", isFull(event, eventId));
        status.put("isBeforeDeadline", isBeforeDeadline(event));
        return status;
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private EventDTO fetchEventSafely(Long eventId) {
        if (eventId == null) return null;
        try { return eventClient.getEventById(eventId); } catch (Exception e) { return null; }
    }

    private UserDTO fetchUserSafely(Long userId) {
        if (userId == null) return null;
        try { return userClient.getUserById(Math.toIntExact(userId)); } catch (Exception e) { return null; }
    }

    private EventInscription buildInscription(InscriptionRequestDTO request, UserDTO user) {
        EventInscription i = new EventInscription();
        i.setParticipantNom(request.getParticipantNom());
        i.setParticipantPrenom(request.getParticipantPrenom());
        i.setParticipantEmail(request.getParticipantEmail() != null
                ? request.getParticipantEmail() : user.getEmail());
        i.setDomaine(request.getDomaine());
        i.setParticipantRole(request.getParticipantRole());
        i.setImageUrl(request.getImageUrl());
        i.setMessage(request.getMessage());
        i.setRegistrationDate(LocalDateTime.now());
        i.setUserId(request.getUserId());
        i.setEventId(request.getEventId());
        return i;
    }

    private String resolveEmail(EventInscription i, UserDTO user) {
        return i.getParticipantEmail() != null ? i.getParticipantEmail()
                : (user != null ? user.getEmail() : null);
    }

    private String fullName(EventInscription i) {
        return i.getParticipantPrenom() + " " + i.getParticipantNom();
    }

    private String eventTitle(EventDTO event) {
        return event != null ? event.getTitle() : "Événement";
    }



    private InscriptionResponseDTO mapToResponse(EventInscription i, EventDTO event, UserDTO user) {
        InscriptionResponseDTO dto = new InscriptionResponseDTO();
        dto.setId(i.getId());
        dto.setParticipantNom(i.getParticipantNom());
        dto.setParticipantPrenom(i.getParticipantPrenom());
        dto.setParticipantEmail(resolveEmail(i, user));
        dto.setRegistrationDate(i.getRegistrationDate());
        dto.setWaitlistDate(i.getWaitlistDate());
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