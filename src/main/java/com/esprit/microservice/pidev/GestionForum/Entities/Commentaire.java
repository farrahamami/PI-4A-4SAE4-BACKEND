package com.esprit.microservice.pidev.GestionForum.Entities;

import com.esprit.microservice.pidev.Entities.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "commentaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenue;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(nullable = false)
    private Integer likes = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"publications", "commentaires", "password", "email"})
    private User user;

    // ✅ SOLUTION : Seulement la relation, PAS de champ séparé
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publication_id", nullable = false)
    @JsonIgnoreProperties("commentaires")
    private Publication publication;

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }
}