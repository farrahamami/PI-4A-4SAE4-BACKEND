package com.esprit.eventservice.services;

import com.esprit.eventservice.clients.ActivityClient;
import com.esprit.eventservice.clients.UserClient;
import com.esprit.eventservice.dto.*;
import com.esprit.eventservice.entities.Event;
import com.esprit.eventservice.entities.EventStatus;
import com.esprit.eventservice.repositories.EventRepository;
import com.esprit.eventservice.specifications.EventSpecification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements IEventService {

    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final GeocodingService geocodingService;
    private final ActivityClient activityClient;   // ← AJOUTÉ

    public EventServiceImpl(EventRepository eventRepository,
                            UserClient userClient,
                            GeocodingService geocodingService,
                            ActivityClient activityClient) {  // ← AJOUTÉ
        this.eventRepository  = eventRepository;
        this.userClient       = userClient;
        this.geocodingService = geocodingService;
        this.activityClient   = activityClient;    // ← AJOUTÉ
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "idEvent", "title", "startDate", "endDate",
            "capacity", "currentParticipants", "createdAt", "location"
    );

    @Override
    public EventResponseDTO addEvent(EventRequestDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        UserDTO user = userClient.getUserById(Math.toIntExact(dto.getUserId()));
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé: " + dto.getUserId());
        }

        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setLocation(dto.getLocation());
        event.setCapacity(dto.getCapacity());
        event.setImageUrl(dto.getImageUrl());
        event.setCategory(dto.getCategory());
        event.setUserId(dto.getUserId());
        event.setCurrentParticipants(0);
        event.setEventStatus(EventStatus.PUBLISHED);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            double[] coords = geocodingService.geocodeAddress(dto.getLocation());
            if (coords != null) {
                event.setLatitude(coords[0]);
                event.setLongitude(coords[1]);
            }
        }

        Event saved = eventRepository.save(event);

        // ── AJOUTÉ : envoie les activités à activity-service ──────────────
        if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {
            for (ActivityDTO actDto : dto.getActivities()) {
                try {
                    actDto.setEventId(saved.getIdEvent()); // lie l'activité à l'event créé
                    activityClient.createActivity(actDto);
                } catch (Exception e) {
                    System.err.println("[EventService] Erreur création activité '" + actDto.getName() + "': " + e.getMessage());
                }
            }
        }
        // ──────────────────────────────────────────────────────────────────

        return mapToResponseDTO(saved, user);
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
        event.setCapacity(dto.getCapacity());
        event.setImageUrl(dto.getImageUrl());
        event.setCategory(dto.getCategory());
        event.setUpdatedAt(LocalDateTime.now());

        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            boolean locationChanged = !dto.getLocation().equals(event.getLocation());
            event.setLocation(dto.getLocation());
            if (locationChanged || event.getLatitude() == null) {
                double[] coords = geocodingService.geocodeAddress(dto.getLocation());
                if (coords != null) {
                    event.setLatitude(coords[0]);
                    event.setLongitude(coords[1]);
                }
            }
        }

        Event updated = eventRepository.save(event);

        // ── Supprime les anciennes activités puis recrée les nouvelles ──────
        if (dto.getActivities() != null) {
            try {
                activityClient.deleteActivitiesByEventId(updated.getIdEvent()); // supprime d'abord
            } catch (Exception e) {
                System.err.println("[EventService] Erreur suppression activités: " + e.getMessage());
            }
            for (ActivityDTO actDto : dto.getActivities()) {
                try {
                    actDto.setEventId(updated.getIdEvent());
                    activityClient.createActivity(actDto);
                } catch (Exception e) {
                    System.err.println("[EventService] Erreur création activité '" + actDto.getName() + "': " + e.getMessage());
                }
            }
        }
        // ──────────────────────────────────────────────────────────────────

        UserDTO user = fetchUserSafely(updated.getUserId());
        return mapToResponseDTO(updated, user);
    }

    @Override
    public EventResponseDTO getEventById(Long idEvent) {
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        UserDTO user = fetchUserSafely(event.getUserId());
        return mapToResponseDTO(event, user);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
        return eventRepository.findByArchivedFalse().stream()
                .map(e -> mapToResponseDTO(e, fetchUserSafely(e.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public void archiveEvent(Long idEvent) {
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        event.setArchived(true);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    @Override
    public List<EventResponseDTO> getArchivedEvents() {
        return eventRepository.findByArchivedTrue().stream()
                .map(e -> mapToResponseDTO(e, fetchUserSafely(e.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public void restoreEvent(Long idEvent) {
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        event.setArchived(false);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    @Override
    public void geocodeAllExistingEvents() {
        List<Event> events = eventRepository.findByLatitudeIsNull();
        for (Event event : events) {
            if (event.getLocation() != null && !event.getLocation().isBlank()) {
                double[] coords = geocodingService.geocodeAddress(event.getLocation());
                if (coords != null) {
                    event.setLatitude(coords[0]);
                    event.setLongitude(coords[1]);
                    eventRepository.save(event);
                }
                try { Thread.sleep(1100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }
    }

    @Override
    public PageResponseDTO<EventResponseDTO> filterEvents(EventFilterDTO filter) {
        String sortField = ALLOWED_SORT_FIELDS.contains(filter.getSortBy()) ? filter.getSortBy() : "idEvent";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int pageSize = Math.min(Math.max(filter.getSize(), 1), 100);
        int pageNum  = Math.max(filter.getPage(), 0);

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        Specification<Event> spec = EventSpecification.buildFromFilter(filter);
        Page<Event> page = eventRepository.findAll(spec, pageable);
        long totalCount  = eventRepository.count();

        List<EventResponseDTO> content = page.getContent().stream()
                .map(e -> mapToResponseDTO(e, fetchUserSafely(e.getUserId())))
                .collect(Collectors.toList());

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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserDTO fetchUserSafely(Long userId) {
        if (userId == null) return null;
        try {
            return userClient.getUserById(Math.toIntExact(userId));
        } catch (Exception e) {
            return null;
        }
    }

    private EventResponseDTO mapToResponseDTO(Event event, UserDTO user) {
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
        dto.setLatitude(event.getLatitude());
        dto.setLongitude(event.getLongitude());
        dto.setArchived(event.isArchived());
        dto.setUserId(event.getUserId());
        if (user != null) {
            dto.setUserName(user.getName() + " " + user.getLastName());
        }
        return dto;
    }
}