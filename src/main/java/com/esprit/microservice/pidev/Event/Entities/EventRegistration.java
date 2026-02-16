package com.esprit.microservice.pidev.Event.Entities;

import com.esprit.microservice.pidev.Entities.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;
     LocalDateTime registrationDate;
    @Enumerated(EnumType.STRING)
    RegistrationStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User participant;

    @ManyToOne
    @JoinColumn(name = "event_id")
    Event event;
}
