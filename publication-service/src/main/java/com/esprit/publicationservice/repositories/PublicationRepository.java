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

    /** Toutes les publications ACTIVES (utilisées dans le feed public) */
    List<Publication> findByStatutOrderByCreateAtDesc(StatutPublication statut);

    /** Publications ACTIVES d'un type donné */
    List<Publication> findByTypeAndStatutOrderByCreateAtDesc(TypePublication type, StatutPublication statut);

    /** Publications archivées ou en attente d'un utilisateur (pour son onglet Archives) */
    List<Publication> findByUserIdAndStatutIn(Integer userId, List<StatutPublication> statuts);

    /** Publications en attente (pour l'admin) */
    List<Publication> findByStatut(StatutPublication statut);

    /** Vérifie si un userId a déjà signalé une publication */
    @Query("SELECT CASE WHEN :userId MEMBER OF p.signalements THEN true ELSE false END FROM Publication p WHERE p.id = :pubId")
    boolean hasUserAlreadySignaled(@Param("pubId") Integer pubId, @Param("userId") Integer userId);
}