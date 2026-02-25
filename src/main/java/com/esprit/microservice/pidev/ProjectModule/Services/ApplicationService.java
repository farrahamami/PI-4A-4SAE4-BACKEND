package com.esprit.microservice.pidev.ProjectModule.Services;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Application;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.FreelancerSkill;
import com.esprit.microservice.pidev.ProjectModule.Domain.Entities.Project;
import com.esprit.microservice.pidev.ProjectModule.Domain.Enums.ApplicationStatus;
import com.esprit.microservice.pidev.ProjectModule.Repositories.ApplicationRepository;
import com.esprit.microservice.pidev.ProjectModule.Repositories.FreelancerSkillRepository;
import com.esprit.microservice.pidev.ProjectModule.Repositories.ProjectRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.FileOutputStream;
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
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FreelancerSkillRepository skillRepository;

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    /**
     * Retourne le chemin ABSOLU du dossier uploads.
     * Utilise le répertoire de travail réel de Spring Boot (user.dir)
     * pour être cohérent sur Windows/Mac/Linux.
     */
    private Path getUploadRootPath() {
        // Si uploadDir est déjà absolu (ex: C:/mon/chemin/uploads/) → on l'utilise tel quel
        // Si relatif (ex: uploads/) → on le résout depuis user.dir (= racine du projet)
        Path p = Paths.get(uploadDir);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Paths.get(System.getProperty("user.dir"), uploadDir).normalize();
    }

    // ─── Soumettre une application ──────────────────────────────────────

    public Application submitApplication(Integer freelancerId, Integer projectId, String coverLetterUrl) {

        if (applicationRepository.findByFreelancerIdAndProjectId(freelancerId, projectId).isPresent()) {
            throw new RuntimeException("Tu as déjà appliqué à ce projet.");
        }

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer introuvable : " + freelancerId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projectId));

        Application app = new Application();
        app.setFreelancer(freelancer);
        app.setProject(project);
        app.setCoverLetterUrl(coverLetterUrl);
        app.setAppliedAt(LocalDate.now());
        app.setStatus(ApplicationStatus.PENDING);

        return applicationRepository.save(app);
    }

    // ─── Upload lettre de motivation PDF ──────────────────────────────

    public String uploadCoverLetter(Integer freelancerId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés.");
        }

        // Chemin absolu garanti
        Path dirPath = getUploadRootPath().resolve("cover-letters");
        Files.createDirectories(dirPath);

        System.out.println(">>> uploadCoverLetter saving to: " + dirPath.toAbsolutePath());

        String fileName = "cover_" + freelancerId + "_" + UUID.randomUUID() + ".pdf";
        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        System.out.println(">>> File saved: " + filePath.toAbsolutePath());

        return "/uploads/cover-letters/" + fileName;
    }

    // ─── Générer lettre de motivation depuis le CV ─────────────────────

    public String generateCoverLetter(Integer freelancerId, Integer projectId) throws IOException {

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer introuvable"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        List<FreelancerSkill> skills = skillRepository.findByFreelancerId(freelancerId);

        // Chemin absolu garanti
        Path dirPath = getUploadRootPath().resolve("cover-letters");
        Files.createDirectories(dirPath);

        String fileName = "cover_generated_" + freelancerId + "_" + UUID.randomUUID() + ".pdf";
        Path filePath = dirPath.resolve(fileName);

        System.out.println(">>> generateCoverLetter saving to: " + filePath.toAbsolutePath());

        // ── Génération PDF avec OpenPDF ──
        Document document = new Document(PageSize.A4, 60, 60, 80, 80);
        PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
        document.open();

        Font titleFont  = new Font(Font.HELVETICA, 18, Font.BOLD,   new Color(30, 90, 160));
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL,  Color.DARK_GRAY);
        Font boldFont   = new Font(Font.HELVETICA, 11, Font.BOLD,    Color.DARK_GRAY);
        Font subFont    = new Font(Font.HELVETICA, 13, Font.BOLD,    new Color(50, 50, 50));

        Paragraph header = new Paragraph(freelancer.getName() + " " + freelancer.getLastName(), titleFont);
        header.setAlignment(Element.ALIGN_LEFT);
        document.add(header);
        document.add(new Paragraph(freelancer.getEmail(), normalFont));
        document.add(new Paragraph(LocalDate.now().toString(), normalFont));
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("─────────────────────────────────────────────", normalFont));
        document.add(Chunk.NEWLINE);

        Paragraph objet = new Paragraph();
        objet.add(new Chunk("Objet : ", boldFont));
        objet.add(new Chunk("Candidature pour le projet « " + project.getTitle() + " »", normalFont));
        document.add(objet);
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Madame, Monsieur,", normalFont));
        document.add(Chunk.NEWLINE);

        String intro = "Je me permets de vous adresser ma candidature pour le projet « "
                + project.getTitle() + " » publié sur votre plateforme. "
                + "Fort(e) de mon expérience en développement et de mes compétences techniques, "
                + "je suis convaincu(e) de pouvoir apporter une contribution significative à votre équipe.";
        document.add(new Paragraph(intro, normalFont));
        document.add(Chunk.NEWLINE);

        if (!skills.isEmpty()) {
            document.add(new Paragraph("Mes compétences clés :", subFont));
            document.add(Chunk.NEWLINE);
            for (FreelancerSkill skill : skills) {
                String line = "• " + skill.getSkillName()
                        + " — Niveau : " + (skill.getLevel() != null ? skill.getLevel().toString() : "N/A")
                        + " — " + (skill.getYearsExperience() != null ? skill.getYearsExperience() : 0)
                        + " an(s) d'expérience";
                document.add(new Paragraph(line, normalFont));
            }
            document.add(Chunk.NEWLINE);
        }

        if (project.getDescription() != null) {
            String projectParagraph = "Votre projet correspond parfaitement à mes domaines d'expertise. "
                    + "Je suis particulièrement motivé(e) par : " + project.getDescription();
            document.add(new Paragraph(projectParagraph, normalFont));
            document.add(Chunk.NEWLINE);
        }

        String conclusion = "Je reste disponible pour tout entretien ou échange complémentaire. "
                + "Dans l'attente de votre retour, je vous adresse mes cordiales salutations.";
        document.add(new Paragraph(conclusion, normalFont));
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph(freelancer.getName() + " " + freelancer.getLastName(), boldFont));

        document.close();

        return "/uploads/cover-letters/" + fileName;
    }

    // ─── Lectures ─────────────────────────────────────────────────────

    public List<Application> getByFreelancer(Integer freelancerId) {
        return applicationRepository.findByFreelancerId(freelancerId);
    }

    public List<Application> getByProject(Integer projectId) {
        return applicationRepository.findByProjectId(projectId);
    }

    public boolean alreadyApplied(Integer freelancerId, Integer projectId) {
        return applicationRepository.findByFreelancerIdAndProjectId(freelancerId, projectId).isPresent();
    }

    public Optional<Application> getById(Integer id) {
        return applicationRepository.findById(id);
    }

    public Application updateStatus(Integer applicationId, ApplicationStatus status) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application introuvable : " + applicationId));
        app.setStatus(status);
        return applicationRepository.save(app);
    }
}