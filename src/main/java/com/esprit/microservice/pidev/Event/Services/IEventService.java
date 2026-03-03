package com.esprit.microservice.pidev.Event.Services;


import com.esprit.microservice.pidev.Event.DTOs.EventFilterDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventResponseDTO;
import com.esprit.microservice.pidev.Event.DTOs.PageResponseDTO;

import java.util.List;
public interface IEventService {
    EventResponseDTO addEvent(EventRequestDTO eventRequestDTO);
    EventResponseDTO updateEvent(Long idEvent, EventRequestDTO eventRequestDTO);
    EventResponseDTO getEventById(Long idEvent);
    List<EventResponseDTO> getAllEvents();


    PageResponseDTO<EventResponseDTO> filterEvents(EventFilterDTO filter);

    void geocodeAllExistingEvents();

    void archiveEvent(Long idEvent);
    List<EventResponseDTO> getArchivedEvents();
    void restoreEvent(Long idEvent);


}
