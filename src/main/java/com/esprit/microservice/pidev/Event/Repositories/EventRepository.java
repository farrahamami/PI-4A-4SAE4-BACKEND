package com.esprit.microservice.pidev.Event.Repositories;

import com.esprit.microservice.pidev.Event.Entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface EventRepository extends JpaRepository<Event, Long>{
    List<Event> findByUser_Id(Integer userId);
}

