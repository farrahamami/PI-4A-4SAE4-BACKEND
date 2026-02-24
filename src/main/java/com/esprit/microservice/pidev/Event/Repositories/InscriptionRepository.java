package com.esprit.microservice.pidev.Event.Repositories;

import com.esprit.microservice.pidev.Event.Entities.EventInscription;
import com.esprit.microservice.pidev.Event.Entities.InscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<EventInscription, Long> {

    @Query("SELECT i FROM EventInscription i WHERE i.event.idEvent = :eventId")
    List<EventInscription> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT i FROM EventInscription i WHERE i.user.id = :userId")
    List<EventInscription> findByUserId(@Param("userId") Long userId);

    @Query("SELECT i FROM EventInscription i WHERE i.event.idEvent = :eventId AND i.status = :status")
    List<EventInscription> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                                  @Param("status") InscriptionStatus status);

    @Query("SELECT COUNT(i) FROM EventInscription i WHERE i.event.idEvent = :eventId AND i.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId,
                                 @Param("status") InscriptionStatus status);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM EventInscription i WHERE i.user.id = :userId AND i.event.idEvent = :eventId")
    boolean existsByUserIdAndEventId(@Param("userId") Long userId,
                                     @Param("eventId") Long eventId);

    @Query("SELECT i FROM EventInscription i WHERE i.event.idEvent = :eventId AND i.status = 'ACCEPTED'")
    List<EventInscription> findAcceptedByEventId(@Param("eventId") Long eventId);
}
