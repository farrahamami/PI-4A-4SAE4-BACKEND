package com.esprit.microservice.pidev.ProjectModule.Repositories;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // Toutes les applications d'un freelancer
    List<Application> findByFreelancerId(Integer freelancerId);

    // Toutes les applications sur un projet
    List<Application> findByProjectId(Integer projectId);

    // Vérifier si un freelancer a déjà appliqué à un projet
    Optional<Application> findByFreelancerIdAndProjectId(Integer freelancerId, Integer projectId);

    // Vérifier si c'est la première application du freelancer (tous projets)
    boolean existsByFreelancerId(Integer freelancerId);
}