package com.esprit.microservice.pidev.Event.DTOs;

import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class EventFilterDTO {

    // ── Filtres texte ──
    private String titleContains;
    private String locationContains;
    private String descriptionContains;

    // ── Filtres enum ──
    private EventStatus status;
    private CategoryEvent category;

    // ── Filtres dates ──
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateTo;

    // ── Filtres capacité ──
    private Integer capacityMin;
    private Integer capacityMax;

    // ── Filtres participants ──
    private Integer participantsMin;
    private Integer participantsMax;

    // ── Filtre organisateur ──
    private Integer userId;

    // ── Tri ──
    private String sortBy   = "idEvent";   // champ de tri par défaut
    private String sortDir  = "desc";      // "asc" ou "desc"

    // ── Pagination ──
    private int page = 0;
    private int size = 10;
}
