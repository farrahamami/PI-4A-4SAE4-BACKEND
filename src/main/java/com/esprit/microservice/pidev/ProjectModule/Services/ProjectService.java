package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.ProjectModule.DTOs.ProjectRequestDTO;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Project;
import com.esprit.microservice.pidev.ProjectModule.Repositories.FreelancerSkillRepository;
import com.esprit.microservice.pidev.ProjectModule.Repositories.ProjectRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FreelancerSkillRepository skillRepository;

    public Project createProject(ProjectRequestDTO dto) {
        Project project = new Project();
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setBudget(dto.getBudget());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setStatus(dto.getStatus());
        project.setCategory(dto.getCategory());
        project.setCreatedAt(LocalDate.now());

        // Set client
        userRepository.findById(dto.getClientId()).ifPresent(project::setClient);

        // Set skills
        if (dto.getSkillIds() != null && !dto.getSkillIds().isEmpty()) {
            List<FreelancerSkill> skills = skillRepository.findAllById(dto.getSkillIds());
            project.setRequiredSkills(skills);
        }

        return projectRepository.save(project);
    }

    public Project updateProject(Integer id, ProjectRequestDTO dto) {
        return projectRepository.findById(id).map(existing -> {
            existing.setTitle(dto.getTitle());
            existing.setDescription(dto.getDescription());
            existing.setBudget(dto.getBudget());
            existing.setStartDate(dto.getStartDate());
            existing.setEndDate(dto.getEndDate());
            existing.setStatus(dto.getStatus());
            existing.setCategory(dto.getCategory());

            userRepository.findById(dto.getClientId()).ifPresent(existing::setClient);

            if (dto.getSkillIds() != null) {
                List<FreelancerSkill> skills = skillRepository.findAllById(dto.getSkillIds());
                existing.setRequiredSkills(skills);
            }

            return projectRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Integer id) {
        return projectRepository.findById(id);
    }

    public void deleteProject(Integer id) {
        projectRepository.deleteById(id);
    }
}
