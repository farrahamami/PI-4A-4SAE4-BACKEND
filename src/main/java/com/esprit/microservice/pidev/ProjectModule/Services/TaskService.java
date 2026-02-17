package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Task;
import com.esprit.microservice.pidev.ProjectModule.Repositories.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * Récupérer les tasks d'un projet
     */
    public List<Task> getTasksByProjectId(Integer projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Récupérer une task par ID
     */
    public Optional<Task> getTaskById(Integer id) {
        return taskRepository.findById(id);
    }

    /**
     * Supprimer une task
     */
    public void deleteTask(Integer id) {
        taskRepository.deleteById(id);
    }
}