package com.esprit.microservice.pidev.ProjectModule.Repositories;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerSkillRepository extends JpaRepository<FreelancerSkill, Integer> {
    // Tous les skills d'un freelancer
    List<FreelancerSkill> findByFreelancerId(Integer freelancerId);

    // Vérifier si le freelancer a déjà des skills (premier apply check)
    boolean existsByFreelancerId(Integer freelancerId);
}
