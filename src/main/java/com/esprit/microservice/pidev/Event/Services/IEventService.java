package com.esprit.microservice.pidev.Event.Services;


import com.esprit.microservice.pidev.Event.DTOs.EventRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventResponseDTO;

import java.util.List;
public interface IEventService {
    EventResponseDTO addEvent(EventRequestDTO eventRequestDTO);
    EventResponseDTO updateEvent(Long idEvent, EventRequestDTO eventRequestDTO);
    EventResponseDTO getEventById(Long idEvent);
    List<EventResponseDTO> getAllEvents();
    void deleteEvent(Long idEvent);
}
