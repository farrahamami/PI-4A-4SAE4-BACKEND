package com.esprit.microservice.pidev.Event.Repositories;

import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import com.esprit.microservice.pidev.Event.Entities.Event;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {          // ← AJOUT ESSENTIEL

    // ── Méthodes existantes ──
    List<Event> findByUser_Id(Integer userId);

    // ── Comptages (pour les stats du filtre) ──
    long countByEventStatus(EventStatus status);
    long countByCategory(CategoryEvent category);

    // ── Requête native pour les stats globales ──
    @Query("SELECT COUNT(e) FROM Event e WHERE e.startDate >= :from AND e.startDate <= :to")
    long countByStartDateBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // ── Recherche full-text simple (fallback sans Specification) ──
    @Query("""
        SELECT e FROM Event e
        WHERE (:keyword IS NULL
               OR LOWER(e.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.location)    LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Event> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ── Pagination simple par statut ──
    Page<Event> findByEventStatus(EventStatus status, Pageable pageable);

    // ── Pagination simple par catégorie ──
    Page<Event> findByCategory(CategoryEvent category, Pageable pageable);

    // ── Événements d'un utilisateur (paginés) ──
    Page<Event> findByUser_Id(Integer userId, Pageable pageable);
}

