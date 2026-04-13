package com.esprit.publicationservice.services;

import com.esprit.publicationservice.clients.UserClient;
import com.esprit.publicationservice.dto.UserBlockDTO;
import com.esprit.publicationservice.dto.UserDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.repositories.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final UserClient userClient;
    private static final String UPLOAD_DIR = "uploads/publications/";
    private static final int SIGNALEMENT_THRESHOLD = 3;
    private static final long BLOCK_THRESHOLD = 3;

    // ── Lecture ──────────────────────────────────────────────────────

    public List<Publication> getAllPublications() {
        List<Publication> list = publicationRepository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getAllPublicationsAdmin() {
        List<Publication> list = publicationRepository.findAllByOrderByCreateAtDesc();
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getPublicationsByType(TypePublication type) {
        List<Publication> list = publicationRepository.findByTypeAndStatutOrderByCreateAtDesc(type, StatutPublication.ACTIVE);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getPublicationsByUserId(Integer userId) {
        List<Publication> list = publicationRepository.findByUserId(userId);
        list.forEach(this::enrichWithUser);
        return list;
    }

    /**
     * Publications archivées de l'utilisateur (côté front : "Mes posts archivés").
     */
    public List<Publication> getArchivedByUserId(Integer userId) {
        List<Publication> list = publicationRepository.findByUserIdAndStatut(userId, StatutPublication.ARCHIVED);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public Publication getPublicationById(Integer id) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found: " + id));
        enrichWithUser(p);
        return p;
    }

    // ── Blocage / Avertissement ──────────────────────────────────────

    /**
     * Un utilisateur est bloqué si le nombre de ses publications ARCHIVED >= 3.
     */
    public boolean isUserBlocked(Integer userId) {
        return publicationRepository.countArchivedByUserId(userId) >= BLOCK_THRESHOLD;
    }

    /**
     * Retourne le nombre de publications archivées de l'utilisateur.
     */
    public long getArchivedCount(Integer userId) {
        return publicationRepository.countArchivedByUserId(userId);
    }

    // ── Réactivation compte (admin) ──────────────────────────────────

    /**
     * L'admin réactive le compte d'un utilisateur :
     * - Supprime TOUTES ses publications archivées (fichiers inclus)
     * - Remet les signalements à zéro sur ses publications restantes
     */
    public void reactiverCompteUser(Integer userId) {
        List<Publication> allUserPubs = publicationRepository.findByUserId(userId);

        // Supprimer les publications archivées avec leurs fichiers
        List<Publication> archivedPubs = allUserPubs.stream()
                .filter(p -> p.getStatut() == StatutPublication.ARCHIVED)
                .collect(Collectors.toList());

        for (Publication p : archivedPubs) {
            p.getImages().forEach(this::deleteFile);
            p.getPdfs().forEach(this::deleteFile);
            publicationRepository.delete(p);
        }

        // Sur les publications restantes (ACTIVE), remettre les signalements à zéro
        List<Publication> activePubs = allUserPubs.stream()
                .filter(p -> p.getStatut() == StatutPublication.ACTIVE)
                .collect(Collectors.toList());

        for (Publication p : activePubs) {
            p.getSignalements().clear();
            p.getSignalementRaisons().clear();
        }
        publicationRepository.saveAll(activePubs);
    }

    // ── Liste des utilisateurs avec statut de blocage ────────────────

    public List<UserBlockDTO> getAllUsersBlockStatus() {
        // Récupère tous les users ayant au moins une publication archivée
        Map<Integer, Long> archivedCountByUser = publicationRepository
                .findAllByOrderByCreateAtDesc()
                .stream()
                .filter(p -> p.getStatut() == StatutPublication.ARCHIVED)
                .collect(Collectors.groupingBy(Publication::getUserId, Collectors.counting()));

        List<UserBlockDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : archivedCountByUser.entrySet()) {
            Integer uid = entry.getKey();
            long count  = entry.getValue();
            String name = "", lastName = "";
            try {
                UserDTO u = userClient.getUserById(uid);
                name     = u.getName()     != null ? u.getName()     : "";
                lastName = u.getLastName() != null ? u.getLastName() : "";
            } catch (Exception ignored) {}
            result.add(new UserBlockDTO(uid, name, lastName, count));
        }

        result.sort(Comparator.comparingLong(UserBlockDTO::getArchivedCount).reversed());
        return result;
    }

    // ── Signalement ──────────────────────────────────────────────────

    /**
     * Signale une publication avec une raison.
     * 3 signalements → archivage automatique.
     */
    public Publication signalerPublication(Integer id, Integer userId, String raison) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found: " + id));

        if (p.getStatut() != StatutPublication.ACTIVE) {
            throw new IllegalStateException("Cette publication n'est pas active.");
        }
        if (p.getSignalements().contains(userId)) {
            throw new IllegalStateException("Vous avez déjà signalé cette publication.");
        }

        p.getSignalements().add(userId);
        p.getSignalementRaisons().add(raison != null ? raison : "");

        if (p.getSignalements().size() >= SIGNALEMENT_THRESHOLD) {
            p.setStatut(StatutPublication.ARCHIVED);
            p.setArchivedAt(java.time.LocalDateTime.now());
        }

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    // ── Admin : accepter / refuser réactivation (supprimé — PENDING retiré) ──
    // Ces méthodes ne sont plus nécessaires car le statut PENDING est supprimé.

    // ── CRUD ─────────────────────────────────────────────────────────

    public Publication createPublication(String titre, String contenue, TypePublication type,
                                         Integer userId, List<MultipartFile> images,
                                         List<MultipartFile> pdfs, String titleColor,
                                         String contentColor, String titleFontSize) throws IOException {
        if (titre == null || titre.trim().isEmpty()) throw new IllegalArgumentException("Title is required");
        if (contenue == null || contenue.trim().isEmpty()) throw new IllegalArgumentException("Content is required");
        try { userClient.getUserById(userId); } catch (Exception e) { throw new RuntimeException("User not found: " + userId); }

        if (isUserBlocked(userId)) {
            throw new IllegalStateException("BLOCKED: Votre compte est bloqué suite à 3 posts signalés. Contactez l'administrateur.");
        }

        if (type == TypePublication.QUESTION) {
            boolean hasImages = images != null && images.stream().anyMatch(f -> !f.isEmpty());
            boolean hasPdfs   = pdfs   != null && pdfs.stream().anyMatch(f -> !f.isEmpty());
            if (hasImages || hasPdfs) throw new IllegalArgumentException("No images/PDFs allowed for QUESTION type.");
        }

        Publication p = new Publication();
        p.setTitre(titre); p.setContenue(contenue); p.setType(type); p.setUserId(userId);
        p.setStatut(StatutPublication.ACTIVE);
        if (titleColor    != null) p.setTitleColor(titleColor);
        if (contentColor  != null) p.setContentColor(contentColor);
        if (titleFontSize != null) p.setTitleFontSize(titleFontSize);

        if (images != null) { List<String> names = new ArrayList<>(); for (MultipartFile f : images) if (!f.isEmpty()) names.add(saveFile(f, false)); p.setImages(names); }
        if (pdfs   != null) { List<String> names = new ArrayList<>(); for (MultipartFile f : pdfs)   if (!f.isEmpty()) names.add(saveFile(f, true));  p.setPdfs(names);   }

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public Publication updatePublication(Integer id, String titre, String contenue, TypePublication type,
                                         Integer userId, List<MultipartFile> newImages, List<String> imagesToKeep,
                                         List<MultipartFile> newPdfs, List<String> pdfsToKeep,
                                         String titleColor, String contentColor, String titleFontSize) throws IOException {
        Publication p = publicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Publication not found"));
        if (!p.getUserId().equals(userId)) throw new RuntimeException("Not authorized");
        if (titre    != null && !titre.trim().isEmpty())    p.setTitre(titre);
        if (contenue != null && !contenue.trim().isEmpty()) p.setContenue(contenue);
        if (type         != null) p.setType(type);
        if (titleColor   != null) p.setTitleColor(titleColor);
        if (contentColor != null) p.setContentColor(contentColor);
        if (titleFontSize!= null) p.setTitleFontSize(titleFontSize);

        List<String> currentImages = new ArrayList<>(p.getImages());
        for (String img : currentImages) if (imagesToKeep == null || !imagesToKeep.contains(img)) deleteFile(img);
        List<String> updatedImages = new ArrayList<>();
        if (imagesToKeep != null) updatedImages.addAll(imagesToKeep);
        if (newImages    != null) for (MultipartFile f : newImages) if (!f.isEmpty()) updatedImages.add(saveFile(f, false));
        p.setImages(updatedImages);

        List<String> currentPdfs = new ArrayList<>(p.getPdfs());
        for (String pdf : currentPdfs) if (pdfsToKeep == null || !pdfsToKeep.contains(pdf)) deleteFile(pdf);
        List<String> updatedPdfs = new ArrayList<>();
        if (pdfsToKeep != null) updatedPdfs.addAll(pdfsToKeep);
        if (newPdfs    != null) for (MultipartFile f : newPdfs) if (!f.isEmpty()) updatedPdfs.add(saveFile(f, true));
        p.setPdfs(updatedPdfs);

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication p = publicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Publication not found"));
        if (!p.getUserId().equals(userId)) throw new RuntimeException("Not authorized");
        p.getImages().forEach(this::deleteFile); p.getPdfs().forEach(this::deleteFile);
        publicationRepository.delete(p);
    }

    public void adminDeletePublication(Integer id) {
        Publication p = publicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Publication not found"));
        p.getImages().forEach(this::deleteFile); p.getPdfs().forEach(this::deleteFile);
        publicationRepository.delete(p);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void enrichWithUser(Publication p) {
        try { p.setUser(userClient.getUserById(p.getUserId())); } catch (Exception ignored) {}
    }

    private String saveFile(MultipartFile file, boolean isPdf) throws IOException {
        String ct = file.getContentType();
        if (isPdf  && (ct == null || !ct.equals("application/pdf"))) throw new IllegalArgumentException("Only PDFs accepted");
        if (!isPdf && (ct == null || !ct.startsWith("image/")))      throw new IllegalArgumentException("Only images accepted");
        String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.copy(file.getInputStream(), path.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    private void deleteFile(String name) {
        try { Files.deleteIfExists(Paths.get(UPLOAD_DIR + name)); } catch (IOException e) { System.err.println("Delete error: " + e.getMessage()); }
    }
}