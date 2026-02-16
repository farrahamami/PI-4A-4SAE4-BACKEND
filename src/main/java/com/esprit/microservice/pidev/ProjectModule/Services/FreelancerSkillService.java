package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Repositories.FreelancerSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FreelancerSkillService {

    private final FreelancerSkillRepository skillRepository;

    public List<FreelancerSkill> getAllSkills() {
        return skillRepository.findAll();
    }

    public Optional<FreelancerSkill> getSkillById(Integer id) {
        return skillRepository.findById(id);
    }

    public FreelancerSkill createSkill(FreelancerSkill skill) {
        return skillRepository.save(skill);
    }

    public FreelancerSkill updateSkill(FreelancerSkill skill) {
        return skillRepository.save(skill);
    }

    public void deleteSkill(Integer id) {
        skillRepository.deleteById(id);
    }
}
