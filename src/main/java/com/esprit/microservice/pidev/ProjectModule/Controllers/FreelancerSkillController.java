package com.esprit.microservice.pidev.ProjectModule.Controllers;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Services.FreelancerSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FreelancerSkillController {

    private final FreelancerSkillService skillService;

    // ─── CRUD de base ──────────────────────────────────────────────────

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

    @PutMapping("/{id}")
    public ResponseEntity<FreelancerSkill> update(
            @PathVariable Integer id,
            @RequestBody FreelancerSkill skill) {
        return skillService.getSkillById(id)
                .map(existing -> {
                    skill.setId(id);
                    return ResponseEntity.ok(skillService.updateSkill(skill));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Par freelancer ────────────────────────────────────────────────

    /**
     * GET /api/skills/freelancer/{freelancerId}
     * Retourne tous les skills d'un freelancer.
     */
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<FreelancerSkill>> getByFreelancer(
            @PathVariable Integer freelancerId) {
        return ResponseEntity.ok(skillService.getSkillsByFreelancer(freelancerId));
    }

    /**
     * POST /api/skills/freelancer/{freelancerId}
     * Crée un skill pour un freelancer donné.
     * Body JSON: { "skillName": "Angular", "level": "INTERMEDIATE", "yearsExperience": 2 }
     */
    @PostMapping("/freelancer/{freelancerId}")
    public ResponseEntity<FreelancerSkill> createForFreelancer(
            @PathVariable Integer freelancerId,
            @RequestBody FreelancerSkill skill) {
        return ResponseEntity.ok(skillService.createSkillForFreelancer(freelancerId, skill));
    }

    /**
     * GET /api/skills/freelancer/{freelancerId}/is-first-apply
     * Retourne true si le freelancer n'a encore aucun skill (= c'est son premier apply).
     * Utilisé par le frontend pour décider d'afficher le setup modal.
     */
    @GetMapping("/freelancer/{freelancerId}/is-first-apply")
    public ResponseEntity<Boolean> isFirstApply(@PathVariable Integer freelancerId) {
        return ResponseEntity.ok(skillService.isFirstApply(freelancerId));
    }

    // ─── Upload du CV PDF ──────────────────────────────────────────────

    /**
     * POST /api/skills/resume/upload/{freelancerId}
     * Reçoit un fichier PDF multipart et le stocke sur le serveur.
     * Retourne l'URL du fichier.
     *
     * Form-data key: "file"
     */
    @PostMapping("/resume/upload/{freelancerId}")
    public ResponseEntity<String> uploadResume(
            @PathVariable Integer freelancerId,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = skillService.uploadResume(freelancerId, file);
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'upload : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}