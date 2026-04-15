package com.esprit.eventservice.specifications;

import com.esprit.eventservice.dto.EventFilterDTO;
import com.esprit.eventservice.entities.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> buildFromFilter(EventFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("archived"), false));

            if (isNotBlank(filter.getTitleContains())) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + filter.getTitleContains().toLowerCase().trim() + "%"));
            }
            if (isNotBlank(filter.getLocationContains())) {
                predicates.add(cb.like(cb.lower(root.get("location")),
                        "%" + filter.getLocationContains().toLowerCase().trim() + "%"));
            }
            if (isNotBlank(filter.getDescriptionContains())) {
                predicates.add(cb.like(cb.lower(root.get("description")),
                        "%" + filter.getDescriptionContains().toLowerCase().trim() + "%"));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("eventStatus"), filter.getStatus()));
            }
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }
            if (filter.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("startDate"), filter.getStartDateFrom()));
            }
            if (filter.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<LocalDateTime>get("startDate"), filter.getStartDateTo()));
            }
            if (filter.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("endDate"), filter.getEndDateFrom()));
            }
            if (filter.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<LocalDateTime>get("endDate"), filter.getEndDateTo()));
            }
            if (filter.getCapacityMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<Integer>get("capacity"), filter.getCapacityMin()));
            }
            if (filter.getCapacityMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<Integer>get("capacity"), filter.getCapacityMax()));
            }
            if (filter.getParticipantsMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<Integer>get("currentParticipants"), filter.getParticipantsMin()));
            }
            if (filter.getParticipantsMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<Integer>get("currentParticipants"), filter.getParticipantsMax()));
            }
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
