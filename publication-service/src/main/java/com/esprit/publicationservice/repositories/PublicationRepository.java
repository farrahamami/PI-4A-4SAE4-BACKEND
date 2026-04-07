package com.esprit.publicationservice.repositories;

import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Integer> {

    List<Publication> findByType(TypePublication type);

    List<Publication> findByUserId(Integer userId);

    List<Publication> findAllByOrderByCreateAtDesc();

    List<Publication> findByStatutOrderByCreateAtDesc(StatutPublication statut);

    List<Publication> findByTypeAndStatutOrderByCreateAtDesc(TypePublication type, StatutPublication statut);

    List<Publication> findByUserIdAndStatutIn(Integer userId, List<StatutPublication> statuts);

    List<Publication> findByStatut(StatutPublication statut);

    @Query("SELECT CASE WHEN :userId MEMBER OF p.signalements THEN true ELSE false END FROM Publication p WHERE p.id = :pubId")
    boolean hasUserAlreadySignaled(@Param("pubId") Integer pubId, @Param("userId") Integer userId);

    /**
     * Ancien count basé sur le statut (gardé pour compatibilité interne si besoin).
     */
    @Query("SELECT COUNT(p) FROM Publication p WHERE p.userId = :userId AND p.statut IN ('ARCHIVED', 'PENDING')")
    long countArchivedByUserId(@Param("userId") Integer userId);

    /**
     * Somme des warningCount de toutes les publications d'un user.
     * Ce total ne décrémente jamais lors d'un déarchivage.
     */
    @Query("SELECT COALESCE(SUM(p.warningCount), 0) FROM Publication p WHERE p.userId = :userId")
    long sumWarningCountByUserId(@Param("userId") Integer userId);
}