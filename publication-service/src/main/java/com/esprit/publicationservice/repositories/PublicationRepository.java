package com.esprit.publicationservice.repositories;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.TypePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PublicationRepository extends JpaRepository<Publication, Integer> {
    List<Publication> findByType(TypePublication type);
    List<Publication> findByUserId(Integer userId);
    List<Publication> findAllByOrderByCreateAtDesc();
}
