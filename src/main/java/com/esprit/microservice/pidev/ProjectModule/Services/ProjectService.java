package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.ProjectModule.DTOs.ProjectRequestDTO;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Project;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Task;
import com.esprit.microservice.pidev.ProjectModule.Repositories.FreelancerSkillRepository;
import com.esprit.microservice.pidev.ProjectModule.Repositories.ProjectRepository;
import com.esprit.microservice.pidev.ProjectModule.Repositories.TaskRepository;
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
    private final TaskRepository taskRepository;

    /**
     * Créer un nouveau projet avec ses tasks
     */
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

        // Set client par clientId
        userRepository.findById(dto.getClientId()).ifPresent(project::setClient);

        // Sauvegarder le projet d'abord
        Project savedProject = projectRepository.save(project);

        // Créer les tasks si elles existent
        if (dto.getTasks() != null && !dto.getTasks().isEmpty()) {
            dto.getTasks().forEach(taskDto -> {
                Task task = new Task();
                task.setTaskName(taskDto.getTaskName());
                task.setDescription(taskDto.getDescription());
                task.setPriority(taskDto.getPriority());
                task.setMilestone(taskDto.getMilestone());
                task.setStartDate(taskDto.getStartDate());
                task.setEndDate(taskDto.getEndDate());
                task.setProject(savedProject); // Lier la task au projet
                taskRepository.save(task);
            });
        }

        // Set skills
        if (dto.getSkillIds() != null && !dto.getSkillIds().isEmpty()) {
            List<FreelancerSkill> skills = skillRepository.findAllById(dto.getSkillIds());
            savedProject.setRequiredSkills(skills);
        }

        return projectRepository.save(savedProject);
    }

    /**
     * Mettre à jour un projet
     */
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

            // Mettre à jour les skills
            if (dto.getSkillIds() != null) {
                List<FreelancerSkill> skills = skillRepository.findAllById(dto.getSkillIds());
                existing.setRequiredSkills(skills);
            }

            // Mettre à jour les tasks
            if (dto.getTasks() != null) {
                // Supprimer les anciennes tasks
                if (existing.getTasks() != null) {
                    existing.getTasks().forEach(task -> taskRepository.deleteById(task.getId()));
                }

                // Créer les nouvelles tasks
                dto.getTasks().forEach(taskDto -> {
                    Task task = new Task();
                    task.setTaskName(taskDto.getTaskName());
                    task.setDescription(taskDto.getDescription());
                    task.setPriority(taskDto.getPriority());
                    task.setMilestone(taskDto.getMilestone());
                    task.setStartDate(taskDto.getStartDate());
                    task.setEndDate(taskDto.getEndDate());
                    task.setProject(existing);
                    taskRepository.save(task);
                });
            }

            return projectRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Project not found"));
    }

    /**
     * Récupérer tous les projets
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Récupérer les projets d'un client spécifique
     */
    public List<Project> getProjectsByClientId(Integer clientId) {
        return projectRepository.findByClientId(clientId);
    }

    /**
     * Récupérer un projet par ID
     */
    public Optional<Project> getProjectById(Integer id) {
        return projectRepository.findById(id);
    }

    /**
     * Supprimer un projet (supprime aussi les tasks associées via CASCADE)
     */
    public void deleteProject(Integer id) {
        projectRepository.deleteById(id);
    }
}