package com.esprit.microservice.pidev.Event.Controllers;

import com.esprit.microservice.pidev.Event.DTOs.EventFilterDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventResponseDTO;
import com.esprit.microservice.pidev.Event.DTOs.PageResponseDTO;
import com.esprit.microservice.pidev.Event.Services.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventController {

    private final IEventService eventService;

    // ══════════════════════════════════════════════════
    //  CRUD EXISTANT (inchangé)
    // ══════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<EventResponseDTO> addEvent(@Valid @RequestBody EventRequestDTO dto) {
        return new ResponseEntity<>(eventService.addEvent(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequestDTO dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // Archive (remplace le DELETE)

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveEvent(@PathVariable Long id) {
        eventService.archiveEvent(id);
        return ResponseEntity.noContent().build(); // ← 204, zéro body
    }

    // Voir les archivés
    @GetMapping("/archived")
    public ResponseEntity<List<EventResponseDTO>> getArchivedEvents() {
        return ResponseEntity.ok(eventService.getArchivedEvents());
    }

    // Restaurer
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restoreEvent(@PathVariable Long id) {
        eventService.restoreEvent(id);
        return ResponseEntity.noContent().build(); // ← 204, zéro body
    }

    @PostMapping("/admin/geocode-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> geocodeAll() {
        eventService.geocodeAllExistingEvents();
        return ResponseEntity.ok("Géocodage terminé");
    }

    // ══════════════════════════════════════════════════
    //  NOUVEAU : FILTRAGE AVANCÉ + PAGINATION
    //
    //  GET /api/events/filter
    //
    //  Paramètres optionnels (tous peuvent être omis) :
    //    titleContains, locationContains, descriptionContains
    //    status       (PUBLISHED | PENDING | CANCELLED | COMPLETED)
    //    category     (CONFERENCE | WORKSHOP | NETWORKING | ...)
    //    startDateFrom, startDateTo  (ISO datetime: 2025-01-01T00:00:00)
    //    endDateFrom,   endDateTo
    //    capacityMin,   capacityMax
    //    participantsMin, participantsMax
    //    userId
    //    sortBy   (idEvent | title | startDate | endDate | capacity | createdAt | location)
    //    sortDir  (asc | desc)
    //    page     (0-based, défaut 0)
    //    size     (défaut 10, max 100)
    // ══════════════════════════════════════════════════

    @GetMapping("/filter")
    public ResponseEntity<PageResponseDTO<EventResponseDTO>> filterEvents(

            // ── Texte ──
            @RequestParam(required = false) String titleContains,
            @RequestParam(required = false) String locationContains,
            @RequestParam(required = false) String descriptionContains,

            // ── Enums ──
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,

            // ── Dates ──
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTo,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTo,

            // ── Capacité / participants ──
            @RequestParam(required = false) Integer capacityMin,
            @RequestParam(required = false) Integer capacityMax,
            @RequestParam(required = false) Integer participantsMin,
            @RequestParam(required = false) Integer participantsMax,

            // ── Organisateur ──
            @RequestParam(required = false) Integer userId,

            // ── Tri ──
            @RequestParam(defaultValue = "idEvent") String sortBy,
            @RequestParam(defaultValue = "desc")    String sortDir,

            // ── Pagination ──
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size

    ) {
        // Construction du DTO filtre
        EventFilterDTO filter = new EventFilterDTO();
        filter.setTitleContains(titleContains);
        filter.setLocationContains(locationContains);
        filter.setDescriptionContains(descriptionContains);

        // Conversion sécurisée des enums
        if (status != null) {
            try {
                filter.setStatus(
                        com.esprit.microservice.pidev.Event.Entities.EventStatus.valueOf(status.toUpperCase())
                );
            } catch (IllegalArgumentException ignored) { /* statut invalide → ignoré */ }
        }
        if (category != null) {
            try {
                filter.setCategory(
                        com.esprit.microservice.pidev.Event.Entities.CategoryEvent.valueOf(category.toUpperCase())
                );
            } catch (IllegalArgumentException ignored) { /* catégorie invalide → ignorée */ }
        }

        filter.setStartDateFrom(startDateFrom);
        filter.setStartDateTo(startDateTo);
        filter.setEndDateFrom(endDateFrom);
        filter.setEndDateTo(endDateTo);
        filter.setCapacityMin(capacityMin);
        filter.setCapacityMax(capacityMax);
        filter.setParticipantsMin(participantsMin);
        filter.setParticipantsMax(participantsMax);
        filter.setUserId(userId);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setPage(page);
        filter.setSize(size);

        return ResponseEntity.ok(eventService.filterEvents(filter));
    }
}