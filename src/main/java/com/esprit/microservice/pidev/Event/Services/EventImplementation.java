package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.DTOs.ActivityRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventResponseDTO;
import com.esprit.microservice.pidev.Event.Entities.Activity;
import com.esprit.microservice.pidev.Event.Entities.Event;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import com.esprit.microservice.pidev.Event.Repositories.EventRepository;
import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class EventImplementation implements IEventService{

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public EventResponseDTO addEvent(EventRequestDTO dto) {

        System.out.println("Nombre d'activités reçues: " +
                (dto.getActivities() != null ? dto.getActivities().size() : "NULL"));

        // Validation
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        // Récupération de l'organisateur
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Création de l'événement
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
                activity.setMaxParticipants(actDto.getMaxParticipants());
                activity.setEvent(saved); // ← utilise "saved" qui a déjà un ID
                return activity;
            }).collect(Collectors.toList());

            saved.getActivities().clear();
            saved.getActivities().addAll(activities);
            eventRepository.save(saved); // ← sauvegarde avec les activités
        }


        return mapToResponseDTO(saved);
    }

    @Override
    public EventResponseDTO updateEvent(Long idEvent, EventRequestDTO dto) {
        // Validation
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        // Récupération de l'événement
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // Mise à jour
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
            event.getActivities().clear(); // supprimer les anciennes activités

            List<Activity> updatedActivities = dto.getActivities().stream().map(actDto -> {
                Activity activity = new Activity();
                activity.setIdActivity(actDto.getIdActivity()); // garde l'ID si existant
                activity.setName(actDto.getName());
                activity.setDescription(actDto.getDescription());
                activity.setRequirements(actDto.getRequirements());
                activity.setMaxParticipants(actDto.getMaxParticipants());
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

    // Méthode de conversion
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
                actDto.setMaxParticipants(act.getMaxParticipants());
                return actDto;
            }).collect(Collectors.toList());
            dto.setActivities(actDtos);
        }

        return dto;
    }
}
