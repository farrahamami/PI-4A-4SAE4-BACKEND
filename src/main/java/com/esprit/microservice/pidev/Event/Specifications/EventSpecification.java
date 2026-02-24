package com.esprit.microservice.pidev.Event.Specifications;

import com.esprit.microservice.pidev.Event.DTOs.EventFilterDTO;
import com.esprit.microservice.pidev.Event.Entities.CategoryEvent;
import com.esprit.microservice.pidev.Event.Entities.Event;
import com.esprit.microservice.pidev.Event.Entities.EventStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    // ─────────────────────────────────────────────
    //  Méthode principale : combine tous les filtres
    // ─────────────────────────────────────────────
    public static Specification<Event> buildFromFilter(EventFilterDTO filter) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ── Titre ──
            if (isNotBlank(filter.getTitleContains())) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + filter.getTitleContains().toLowerCase().trim() + "%"
                ));
            }

            // ── Lieu ──
            if (isNotBlank(filter.getLocationContains())) {
                predicates.add(cb.like(
                        cb.lower(root.get("location")),
                        "%" + filter.getLocationContains().toLowerCase().trim() + "%"
                ));
            }

            // ── Description ──
            if (isNotBlank(filter.getDescriptionContains())) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + filter.getDescriptionContains().toLowerCase().trim() + "%"
                ));
            }

            // ── Statut ──
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("eventStatus"), filter.getStatus()));
            }

            // ── Catégorie ──
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // ── Date début (fourchette) ──
            if (filter.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("startDate"), filter.getStartDateFrom()
                ));
            }
            if (filter.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("startDate"), filter.getStartDateTo()
                ));
            }

            // ── Date fin (fourchette) ──
            if (filter.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("endDate"), filter.getEndDateFrom()
                ));
            }
            if (filter.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("endDate"), filter.getEndDateTo()
                ));
            }

            // ── Capacité ──
            if (filter.getCapacityMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("capacity"), filter.getCapacityMin()
                ));
            }
            if (filter.getCapacityMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("capacity"), filter.getCapacityMax()
                ));
            }

            // ── Participants actuels ──
            if (filter.getParticipantsMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("currentParticipants"), filter.getParticipantsMin()
                ));
            }
            if (filter.getParticipantsMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("currentParticipants"), filter.getParticipantsMax()
                ));
            }

            // ── Organisateur (userId) ──
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(
                        root.get("user").get("id"), filter.getUserId()
                ));
            }

            // Éviter les doublons sur les JOIN (ex: activités)
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ─────────────────────────────────────────────
    //  Specifications individuelles (réutilisables)
    // ─────────────────────────────────────────────

    public static Specification<Event> titleContains(String keyword) {
        return (root, query, cb) -> isNotBlank(keyword)
                ? cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase().trim() + "%")
                : null;
    }

    public static Specification<Event> locationContains(String keyword) {
        return (root, query, cb) -> isNotBlank(keyword)
                ? cb.like(cb.lower(root.get("location")), "%" + keyword.toLowerCase().trim() + "%")
                : null;
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> status != null
                ? cb.equal(root.get("eventStatus"), status)
                : null;
    }

    public static Specification<Event> hasCategory(CategoryEvent category) {
        return (root, query, cb) -> category != null
                ? cb.equal(root.get("category"), category)
                : null;
    }

    public static Specification<Event> startDateFrom(LocalDateTime from) {
        return (root, query, cb) -> from != null
                ? cb.greaterThanOrEqualTo(root.get("startDate"), from)
                : null;
    }

    public static Specification<Event> startDateTo(LocalDateTime to) {
        return (root, query, cb) -> to != null
                ? cb.lessThanOrEqualTo(root.get("startDate"), to)
                : null;
    }

    public static Specification<Event> capacityBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("capacity"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("capacity"), min);
            return cb.between(root.get("capacity"), min, max);
        };
    }

    public static Specification<Event> belongsToUser(Integer userId) {
        return (root, query, cb) -> userId != null
                ? cb.equal(root.get("user").get("id"), userId)
                : null;
    }

    // ─────────────────────────────────────────────
    //  Utilitaire
    // ─────────────────────────────────────────────
    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
