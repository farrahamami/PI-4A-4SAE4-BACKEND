package com.esprit.eventservice.dto;

import com.esprit.eventservice.entities.CategoryEvent;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class EventRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endDate;

    @NotBlank(message = "Le lieu est obligatoire")
    private String location;

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité doit être au moins 1")
    private Integer capacity;

    private String imageUrl;

    @NotNull(message = "La catégorie est obligatoire")
    private CategoryEvent category;

    @NotNull(message = "L'organisateur est obligatoire")
    private Long userId;

    private List<ActivityDTO> activities;

    public EventRequestDTO() {}

    public EventRequestDTO(String title, String description, LocalDateTime startDate, LocalDateTime endDate, String location, Integer capacity, String imageUrl, CategoryEvent category, Long userId, List<ActivityDTO> activities) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
        this.category = category;
        this.userId = userId;
        this.activities = activities;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public CategoryEvent getCategory() {
        return category;
    }

    public void setCategory(CategoryEvent category) {
        this.category = category;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<ActivityDTO> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityDTO> activities) {
        this.activities = activities;
    }
}
