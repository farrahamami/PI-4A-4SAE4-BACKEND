package com.esprit.microservice.pidev.ProjectModule.Repositories;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task,Integer> {
    List<Task> findByProjectId(Integer projectId);
}
