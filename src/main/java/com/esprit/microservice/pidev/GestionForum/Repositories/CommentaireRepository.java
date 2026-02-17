package com.esprit.microservice.pidev.GestionForum.Repositories;

import com.esprit.microservice.pidev.GestionForum.Entities.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Integer> {

    List<Commentaire> findByPublicationId(Integer publicationId);

    List<Commentaire> findAllByOrderByCreateAtDesc();

    // Commentaires racine (sans parent) pour une publication, triés par date
    @Query("SELECT c FROM Commentaire c WHERE c.publication.id = :publicationId AND c.parent IS NULL ORDER BY c.createAt ASC")
    List<Commentaire> findRootCommentairesByPublicationId(@Param("publicationId") Integer publicationId);

    // ✅ Ancien alias gardé pour compatibilité
    @Query("SELECT c FROM Commentaire c WHERE c.publication.id = :publicationId AND c.parent IS NULL ORDER BY c.createAt ASC")
    List<Commentaire> findByPublicationIdOrderByCreateAtAsc(@Param("publicationId") Integer publicationId);

    // Réponses directes à un commentaire
    @Query("SELECT c FROM Commentaire c WHERE c.parent.id = :parentId ORDER BY c.createAt ASC")
    List<Commentaire> findRepliesByParentId(@Param("parentId") Integer parentId);
}