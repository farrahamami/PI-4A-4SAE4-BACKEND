package com.esprit.microservice.pidev.Event.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private List<T> content;

    // ── Infos pagination ──
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    // ── Infos tri actif ──
    private String sortBy;
    private String sortDir;

    // ── Statistiques résultat ──
    private long filteredCount;   // nb résultats avec filtres
    private long totalCount;      // nb total en base
}
