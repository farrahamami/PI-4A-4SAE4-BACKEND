package com.esprit.microservice.pidev.GestionForum.Repositories;

import com.esprit.microservice.pidev.GestionForum.Entities.Publication;
import com.esprit.microservice.pidev.GestionForum.Entities.TypePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Integer> {
    
    List<Publication> findByType(TypePublication type);
    
    List<Publication> findByUserId(Integer userId);
    
    List<Publication> findAllByOrderByCreateAtDesc();
}
