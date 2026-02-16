package com.esprit.microservice.pidev.Entities;

import com.esprit.microservice.pidev.Event.Entities.Event;
import com.esprit.microservice.pidev.Event.Entities.EventRegistration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String lastName;
    private LocalDate birthDate;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    @OneToMany(mappedBy = "participant")
    private List<EventRegistration> eventRegistrations = new ArrayList<>();


    @OneToMany(mappedBy = "organizer")
    private List<Event> organizedEvents = new ArrayList<>();



}
