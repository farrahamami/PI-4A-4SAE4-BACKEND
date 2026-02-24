package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.DTOs.ActivityRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventFilterDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventResponseDTO;
import com.esprit.microservice.pidev.Event.DTOs.PageResponseDTO;
import com.esprit.microservice.pidev.Event.Entities.Activity;
import com.esprit.microservice.pidev.Event.Entities.Event;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import com.esprit.microservice.pidev.Event.Repositories.EventRepository;
import com.esprit.microservice.pidev.Event.Specifications.EventSpecification;
import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventImplementation implements IEventService {

    private final EventRepository eventRepository;
    private final UserRepository  userRepository;

    // Champs autorisés pour le tri (sécurité anti-injection)
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "idEvent", "title", "startDate", "endDate",
            "capacity", "currentParticipants", "createdAt", "location"
    );

    // ══════════════════════════════════════════════════
    //  CRUD EXISTANT (inchangé)
    // ══════════════════════════════════════════════════

    @Override
    public EventResponseDTO addEvent(EventRequestDTO dto) {

        System.out.println("Nombre d'activités reçues: " +
                (dto.getActivities() != null ? dto.getActivities().size() : "NULL"));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setLocation(dto.getLocation());
        event.setCapacity(dto.getCapacity());
        event.setImageUrl(dto.getImageUrl());
        event.setCategory(dto.getCategory());
        event.setUser(user);
        event.setCurrentParticipants(0);
        event.setEventStatus(EventStatus.PUBLISHED);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        Event saved = eventRepository.save(event);

        if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {
            List<Activity> activities = dto.getActivities().stream().map(actDto -> {
                Activity activity = new Activity();
                activity.setName(actDto.getName());
                activity.setDescription(actDto.getDescription());
                activity.setRequirements(actDto.getRequirements());
                activity.setEvent(saved);
                return activity;
            }).collect(Collectors.toList());

            saved.getActivities().clear();
            saved.getActivities().addAll(activities);
            eventRepository.save(saved);
        }

        return mapToResponseDTO(saved);
    }

    @Override
    public EventResponseDTO updateEvent(Long idEvent, EventRequestDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setLocation(dto.getLocation());
        event.setCapacity(dto.getCapacity());
        event.setImageUrl(dto.getImageUrl());
        event.setCategory(dto.getCategory());
        event.setUpdatedAt(LocalDateTime.now());

        if (dto.getActivities() != null) {
            event.getActivities().clear();
            List<Activity> updatedActivities = dto.getActivities().stream().map(actDto -> {
                Activity activity = new Activity();
                activity.setIdActivity(actDto.getIdActivity());
                activity.setName(actDto.getName());
                activity.setDescription(actDto.getDescription());
                activity.setRequirements(actDto.getRequirements());
                activity.setEvent(event);
                return activity;
            }).collect(Collectors.toList());
            event.getActivities().addAll(updatedActivities);
        }

        Event updated = eventRepository.save(event);
        return mapToResponseDTO(updated);
    }

    @Override
    public EventResponseDTO getEventById(Long idEvent) {
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        return mapToResponseDTO(event);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteEvent(Long idEvent) {
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        if (event.getCurrentParticipants() > 0) {
            throw new RuntimeException("Impossible de supprimer un événement avec des participants");
        }
        eventRepository.delete(event);
    }

    // ══════════════════════════════════════════════════
    //  NOUVEAU : FILTRAGE AVANCÉ + PAGINATION
    // ══════════════════════════════════════════════════

    @Override
    public PageResponseDTO<EventResponseDTO> filterEvents(EventFilterDTO filter) {

        // ── 1. Validation et sécurisation du champ de tri ──
        String sortField = ALLOWED_SORT_FIELDS.contains(filter.getSortBy())
                ? filter.getSortBy()
                : "idEvent";

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // ── 2. Construction du Pageable ──
        int pageSize = Math.min(Math.max(filter.getSize(), 1), 100); // entre 1 et 100
        int pageNum  = Math.max(filter.getPage(), 0);

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));

        // ── 3. Construction de la Specification ──
        Specification<Event> spec = EventSpecification.buildFromFilter(filter);

        // ── 4. Exécution de la requête paginée ──
        Page<Event> page = eventRepository.findAll(spec, pageable);

        // ── 5. Comptage total (sans filtres) pour les stats ──
        long totalCount = eventRepository.count();

        // ── 6. Mapping vers DTOs ──
        List<EventResponseDTO> content = page.getContent()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        // ── 7. Construction de la réponse enrichie ──
        PageResponseDTO<EventResponseDTO> response = new PageResponseDTO<>();
        response.setContent(content);
        response.setCurrentPage(page.getNumber());
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        response.setSortBy(sortField);
        response.setSortDir(filter.getSortDir());
        response.setFilteredCount(page.getTotalElements());
        response.setTotalCount(totalCount);

        return response;
    }

    // ══════════════════════════════════════════════════
    //  MAPPING (inchangé)
    // ══════════════════════════════════════════════════

    private EventResponseDTO mapToResponseDTO(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setIdEvent(event.getIdEvent());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setEventStatus(event.getEventStatus());
        dto.setLocation(event.getLocation());
        dto.setCapacity(event.getCapacity());
        dto.setCurrentParticipants(event.getCurrentParticipants());
        dto.setImageUrl(event.getImageUrl());
        dto.setCategory(event.getCategory());
        dto.setCreatedAt(event.getCreatedAt());

        if (event.getUser() != null) {
            dto.setUserId(event.getUser().getId());
            dto.setUserName(event.getUser().getName() + " " + event.getUser().getLastName());
        }

        if (event.getActivities() != null) {
            List<ActivityRequestDTO> actDtos = event.getActivities().stream().map(act -> {
                ActivityRequestDTO actDto = new ActivityRequestDTO();
                actDto.setIdActivity(act.getIdActivity());
                actDto.setName(act.getName());
                actDto.setDescription(act.getDescription());
                actDto.setRequirements(act.getRequirements());
                return actDto;
            }).collect(Collectors.toList());
            dto.setActivities(actDtos);
        }

        return dto;
    }
}