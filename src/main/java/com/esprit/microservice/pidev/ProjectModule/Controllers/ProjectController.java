package com.esprit.microservice.pidev.ProjectModule.Controllers;

import com.esprit.microservice.pidev.ProjectModule.DTOs.ProjectRequestDTO;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Project;
import com.esprit.microservice.pidev.ProjectModule.Repositories.ProjectRepository;
import com.esprit.microservice.pidev.ProjectModule.Services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;

    /**
     * Créer un nouveau projet
     * POST /api/projects
     */
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody ProjectRequestDTO dto) {
        Project project = projectService.createProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    /**
     * Récupérer tous les projets
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Récupérer les projets du client connecté
     * GET /api/projects/my-projects/{clientId}
     */
    @GetMapping("/my-projects/{clientId}")
    public ResponseEntity<List<Project>> getMyProjects(@PathVariable Integer clientId) {
        List<Project> projects = projectService.getProjectsByClientId(clientId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Récupérer un projet par ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Integer id) {
        Optional<Project> project = projectService.getProjectById(id);
        return project.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Mettre à jour un projet (CLIENT ONLY)
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Integer id,
            @RequestBody ProjectRequestDTO dto) {
        Project project = projectService.updateProject(id, dto);
        return ResponseEntity.ok(project);
    }

    /**
     * Supprimer un projet (CLIENT ONLY)
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
