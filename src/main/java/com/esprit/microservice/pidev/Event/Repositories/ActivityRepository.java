package com.esprit.microservice.pidev.Event.Repositories;

import com.esprit.microservice.pidev.Event.Entities.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>{
    List<Activity> findByEventIdEvent(Long eventId);

}
