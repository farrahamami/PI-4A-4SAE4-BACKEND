package com.esprit.microservice.pidev.ProjectModule.Controllers;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Services.FreelancerSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FreelancerSkillController {
    private final FreelancerSkillService skillService;

    @GetMapping
    public List<FreelancerSkill> getAll() {
        return skillService.getAllSkills();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FreelancerSkill> getById(@PathVariable Integer id) {
        return skillService.getSkillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public FreelancerSkill create(@RequestBody FreelancerSkill skill) {
        return skillService.createSkill(skill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FreelancerSkill> update(@PathVariable Integer id, @RequestBody FreelancerSkill skill) {
        if (!skillService.getSkillById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        skill.setId(id);
        return ResponseEntity.ok(skillService.updateSkill(skill));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
