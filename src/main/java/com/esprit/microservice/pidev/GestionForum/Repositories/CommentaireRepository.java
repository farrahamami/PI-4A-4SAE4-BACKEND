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

    // ✅ Requête avec publication.id
    @Query("SELECT c FROM Commentaire c WHERE c.publication.id = :publicationId ORDER BY c.createAt ASC")
    List<Commentaire> findByPublicationIdOrderByCreateAtAsc(@Param("publicationId") Integer publicationId);

}
