package com.esprit.microservice.pidev.Event.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;



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
    String requirements;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;









}
