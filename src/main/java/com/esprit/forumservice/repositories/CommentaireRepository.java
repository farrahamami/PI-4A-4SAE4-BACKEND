package com.esprit.forumservice.repositories;

import com.esprit.forumservice.entities.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Integer> {

    List<Commentaire> findByPublicationId(Integer publicationId);

    List<Commentaire> findAllByOrderByCreateAtDesc();

    @Query("SELECT c FROM Commentaire c WHERE c.publication.id = :publicationId AND c.parent IS NULL ORDER BY c.createAt ASC")
    List<Commentaire> findRootCommentairesByPublicationId(@Param("publicationId") Integer publicationId);

    @Query("SELECT c FROM Commentaire c WHERE c.publication.id = :publicationId AND c.parent IS NULL ORDER BY c.pinned DESC, c.createAt ASC")
    List<Commentaire> findRootCommentairesByPublicationIdOrderByPinned(@Param("publicationId") Integer publicationId);
}
