package com.esprit.microservice.pidev.GestionForum.Entities;

import com.esprit.microservice.pidev.Entities.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenue;

    private String image;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(nullable = false)
    private Integer likes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePublication type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"publications", "commentaires", "password", "email"})
    private User user;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("publication")
    private List<Commentaire> commentaires = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }



}
