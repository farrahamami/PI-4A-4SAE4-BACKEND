package com.esprit.microservice.pidev.ProjectModule.Controllers;

import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Application;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.ApplicationStatus;
import com.esprit.microservice.pidev.ProjectModule.Services.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

    // ──────────────────────────────────────────────────────────────────
    // POST /api/applications
    // Soumettre une application avec une coverLetterUrl déjà connue
    // (après avoir uploadé ou généré la lettre au préalable).
    //
    // Body JSON:
    // {
    //   "freelancerId": 1,
    //   "projectId": 3,
    //   "coverLetterUrl": "/uploads/cover-letters/cover_1_xxx.pdf"
    // }
    // ──────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> submitApplication(@RequestBody Map<String, Object> body) {
        try {
            Integer freelancerId = (Integer) body.get("freelancerId");
            Integer projectId    = (Integer) body.get("projectId");
            String  coverUrl     = (String)  body.get("coverLetterUrl");

            Application app = applicationService.submitApplication(freelancerId, projectId, coverUrl);
            return ResponseEntity.ok(app);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/applications/freelancer/{freelancerId}
    // Toutes les applications d'un freelancer (ses candidatures)
    // ──────────────────────────────────────────────────────────────────
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<Application>> getByFreelancer(@PathVariable Integer freelancerId) {
        return ResponseEntity.ok(applicationService.getByFreelancer(freelancerId));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/applications/project/{projectId}
    // Toutes les applications sur un projet (côté client/admin)
    // ──────────────────────────────────────────────────────────────────
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Application>> getByProject(@PathVariable Integer projectId) {
        return ResponseEntity.ok(applicationService.getByProject(projectId));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/applications/check?freelancerId=1&projectId=3
    // Vérifie si le freelancer a déjà appliqué à ce projet
    // Retourne: true / false
    // ──────────────────────────────────────────────────────────────────
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkAlreadyApplied(
            @RequestParam Integer freelancerId,
            @RequestParam Integer projectId) {
        return ResponseEntity.ok(applicationService.alreadyApplied(freelancerId, projectId));
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /api/applications/cover-letter/upload/{freelancerId}
    // Upload d'un PDF de lettre de motivation depuis le PC
    // Form-data key: "file"
    // Retourne: URL du fichier
    // ──────────────────────────────────────────────────────────────────
    @PostMapping("/cover-letter/upload/{freelancerId}")
    public ResponseEntity<String> uploadCoverLetter(
            @PathVariable Integer freelancerId,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = applicationService.uploadCoverLetter(freelancerId, file);
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur upload : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /api/applications/generate-cover-letter
    // Génère automatiquement une lettre de motivation PDF
    // depuis les skills et infos du freelancer + description du projet.
    //
    // Body JSON:
    // { "freelancerId": 1, "projectId": 3 }
    //
    // Retourne: URL du PDF généré
    // ──────────────────────────────────────────────────────────────────
    @PostMapping("/generate-cover-letter")
    public ResponseEntity<String> generateCoverLetter(@RequestBody Map<String, Integer> body) {
        try {
            Integer freelancerId = body.get("freelancerId");
            Integer projectId    = body.get("projectId");
            String url = applicationService.generateCoverLetter(freelancerId, projectId);
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur génération PDF : " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // PATCH /api/applications/{id}/status
    // Mettre à jour le statut d'une application (côté client)
    // Body JSON: { "status": "ACCEPTED" }
    // ──────────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            ApplicationStatus status = ApplicationStatus.valueOf(body.get("status"));
            Application updated = applicationService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Statut invalide. Valeurs acceptées : PENDING, ACCEPTED, REJECTED");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
