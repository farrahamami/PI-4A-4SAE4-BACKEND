package com.esprit.microservice.pidev.Event.Entities;


import com.esprit.microservice.pidev.Entities.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long idEvent;
    String title;
    String description;
    LocalDateTime startDate;
    LocalDateTime endDate;
    @Enumerated(EnumType.STRING)
    EventStatus eventStatus;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String location;
    int capacity;
    int currentParticipants;
    String imageUrl;
    @Enumerated(EnumType.STRING)
    CategoryEvent category;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "event")
    private List<EventRegistration> registrations = new ArrayList<>();

    // Dans Event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;





}
