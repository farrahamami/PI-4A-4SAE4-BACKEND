package com.esprit.eventservice.specifications;

import com.esprit.eventservice.dto.EventFilterDTO;
import com.esprit.eventservice.entities.CategoryEvent;
import com.esprit.eventservice.entities.EventStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventSpecificationTest {

    // ════════════════════════════════════════════════════════════════════════
    // buildFromFilter() — cas de base
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("buildFromFilter - filtre vide : retourne une Specification non nulle")
    void buildFromFilter_emptyFilter_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        Specification result = EventSpecification.buildFromFilter(filter);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec titleContains : Specification créée")
    void buildFromFilter_withTitle_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setTitleContains("DevOps");
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec locationContains : Specification créée")
    void buildFromFilter_withLocation_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setLocationContains("Tunis");
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec descriptionContains : Specification créée")
    void buildFromFilter_withDescription_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setDescriptionContains("conférence");
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec status : Specification créée")
    void buildFromFilter_withStatus_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setStatus(EventStatus.PUBLISHED);
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec category : Specification créée")
    void buildFromFilter_withCategory_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setCategory(CategoryEvent.CONFERENCE);
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec dates startDate : Specification créée")
    void buildFromFilter_withStartDates_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setStartDateFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        filter.setStartDateTo(LocalDateTime.of(2026, 12, 31, 0, 0));
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec dates endDate : Specification créée")
    void buildFromFilter_withEndDates_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setEndDateFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        filter.setEndDateTo(LocalDateTime.of(2026, 12, 31, 0, 0));
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec capacité min/max : Specification créée")
    void buildFromFilter_withCapacity_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setCapacityMin(10);
        filter.setCapacityMax(200);
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec participants min/max : Specification créée")
    void buildFromFilter_withParticipants_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setParticipantsMin(5);
        filter.setParticipantsMax(50);
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }

    @Test
    @DisplayName("buildFromFilter - avec userId : Specification créée")
    void buildFromFilter_withUserId_returnsSpec() {
        EventFilterDTO filter = new EventFilterDTO();
        filter.setUserId(1L);
        assertThat(EventSpecification.buildFromFilter(filter)).isNotNull();
    }
}