package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Repositories.FreelancerSkillRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreelancerSkillService {

    private final FreelancerSkillRepository skillRepository;
    private final UserRepository userRepository;

    // Dossier où on stocke les fichiers uploadés
    // Défini dans application.properties : file.upload-dir=uploads/
    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    // ─── CRUD de base ──────────────────────────────────────────────────

    public List<FreelancerSkill> getAllSkills() {
        return skillRepository.findAll();
    }

    public Optional<FreelancerSkill> getSkillById(Integer id) {
        return skillRepository.findById(id);
    }

    public FreelancerSkill updateSkill(FreelancerSkill skill) {
        return skillRepository.save(skill);
    }

    public void deleteSkill(Integer id) {
        skillRepository.deleteById(id);
    }

    // ─── Par freelancer ────────────────────────────────────────────────

    /**
     * Retourne tous les skills d'un freelancer donné.
     */
    public List<FreelancerSkill> getSkillsByFreelancer(Integer freelancerId) {
        return skillRepository.findByFreelancerId(freelancerId);
    }

    /**
     * Crée un skill et l'associe au freelancer.
     * Lève une exception si le freelancer n'existe pas.
     */
    public FreelancerSkill createSkillForFreelancer(Integer freelancerId, FreelancerSkill skill) {
        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer introuvable : " + freelancerId));

        skill.setFreelancer(freelancer);
        skill.setCreatedAt(LocalDate.now());
        return skillRepository.save(skill);
    }

    /**
     * Vérifie si c'est le premier skill (donc premier apply) du freelancer.
     */
    public boolean isFirstApply(Integer freelancerId) {
        return !skillRepository.existsByFreelancerId(freelancerId);
    }

    // ─── Upload du CV/Resume PDF ────────────────────────────────────────

    /**
     * Sauvegarde le fichier PDF sur le disque.
     * Met à jour le resumeUrl du premier skill du freelancer (ou tous si tu veux).
     * Retourne l'URL publique du fichier.
     */
    public String uploadResume(Integer freelancerId, MultipartFile file) throws IOException {

        // Vérification : fichier PDF uniquement
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés.");
        }

        // Créer le dossier s'il n'existe pas
        Path dirPath = Paths.get(uploadDir + "resumes/");
        Files.createDirectories(dirPath);

        // Nom unique pour éviter les conflits
        String fileName = "resume_" + freelancerId + "_" + UUID.randomUUID() + ".pdf";
        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // URL accessible publiquement (Spring sert le dossier uploads/ en static)
        String resumeUrl = "/uploads/resumes/" + fileName;

        // Mettre à jour le resumeUrl sur tous les skills du freelancer
        List<FreelancerSkill> skills = skillRepository.findByFreelancerId(freelancerId);
        for (FreelancerSkill s : skills) {
            s.setResumeUrl(resumeUrl);
            skillRepository.save(s);
        }

        return resumeUrl;
    }
}