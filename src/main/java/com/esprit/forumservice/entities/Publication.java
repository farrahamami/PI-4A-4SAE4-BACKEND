package com.esprit.forumservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    // Stocke userId (pas de FK cross-service)
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // Champ transient pour exposer les infos user (rempli par le service)
    @Transient
    private Object user;

    @ElementCollection
    @CollectionTable(name = "publication_images", joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "image_name")
    private List<String> images = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "publication_pdfs", joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "pdf_name")
    private List<String> pdfs = new ArrayList<>();

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "title_color", length = 20)
    private String titleColor = "#2d1f4e";

    @Column(name = "content_color", length = 20)
    private String contentColor = "#6b5e8e";

    @Column(name = "title_font_size", length = 10)
    private String titleFontSize = "1.1rem";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePublication type;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("publication")
    private List<Commentaire> commentaires = new ArrayList<>();

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("publication")
    private List<Reaction> reactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }
}
