package com.esprit.microservice.pidev.ProjectModule.Repositories;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FreelancerSkillRepository extends JpaRepository<FreelancerSkill, Integer> {
}
