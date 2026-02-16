package com.esprit.microservice.pidev.Event.Entities;

import com.esprit.microservice.pidev.Activity.Entities.ActivityStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idActivity;
    String name;
    String description;
    LocalDateTime startTime;
    LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    ActivityStatus activityStatus;
    int duration;
    String requirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;









}
