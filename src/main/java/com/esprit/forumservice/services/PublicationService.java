package com.esprit.forumservice.services;

import com.esprit.forumservice.clients.UserClient;
import com.esprit.forumservice.dto.UserDTO;
import com.esprit.forumservice.entities.Publication;
import com.esprit.forumservice.entities.TypePublication;
import com.esprit.forumservice.repositories.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final UserClient userClient;

    private static final String UPLOAD_DIR =
            new java.io.File("uploads/publications").getAbsolutePath() + "/";
    private static final int MAX_IMAGES = 5;
    private static final int MAX_PDFS = 5;

    public List<Publication> getAllPublications() {
        List<Publication> publications = publicationRepository.findAllByOrderByCreateAtDesc();
        enrichWithUser(publications);
        return publications;
    }

    public List<Publication> getPublicationsByType(TypePublication type) {
        List<Publication> publications = publicationRepository.findByType(type);
        enrichWithUser(publications);
        return publications;
    }

    public List<Publication> getPublicationsByUserId(Integer userId) {
        List<Publication> publications = publicationRepository.findByUserId(userId);
        enrichWithUser(publications);
        return publications;
    }

    public Publication getPublicationById(Integer id) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found: " + id));
        enrichWithUser(p);
        return p;
    }

    public Publication createPublication(String titre, String contenue, TypePublication type,
                                         Integer userId, List<MultipartFile> images,
                                         List<MultipartFile> pdfs,
                                         String titleColor, String contentColor, String titleFontSize) throws IOException {
        if (titre == null || titre.trim().isEmpty())
            throw new IllegalArgumentException("Title is required");
        if (contenue == null || contenue.trim().isEmpty())
            throw new IllegalArgumentException("Content is required");

        // Validate user exists via Feign
        try { userClient.getUserById(userId); }
        catch (Exception e) { throw new RuntimeException("User not found: " + userId); }

        if (type == TypePublication.QUESTION) {
            boolean hasImages = images != null && images.stream().anyMatch(f -> !f.isEmpty());
            boolean hasPdfs   = pdfs   != null && pdfs.stream().anyMatch(f -> !f.isEmpty());
            if (hasImages || hasPdfs)
                throw new IllegalArgumentException("Images and PDFs are not allowed for Question type posts.");
        }

        Publication publication = new Publication();
        publication.setTitre(titre);
        publication.setContenue(contenue);
        publication.setType(type);
        publication.setUserId(userId);
        if (titleColor    != null) publication.setTitleColor(titleColor);
        if (contentColor  != null) publication.setContentColor(contentColor);
        if (titleFontSize != null) publication.setTitleFontSize(titleFontSize);

        if (images != null && !images.isEmpty()) {
            List<String> imageNames = new ArrayList<>();
            for (MultipartFile image : images)
                if (!image.isEmpty()) imageNames.add(saveFile(image, false));
            publication.setImages(imageNames);
        }
        if (pdfs != null && !pdfs.isEmpty()) {
            List<String> pdfNames = new ArrayList<>();
            for (MultipartFile pdf : pdfs)
                if (!pdf.isEmpty()) pdfNames.add(saveFile(pdf, true));
            publication.setPdfs(pdfNames);
        }

        Publication saved = publicationRepository.save(publication);
        enrichWithUser(saved);
        return saved;
    }

    public Publication updatePublication(Integer id, String titre, String contenue,
                                         TypePublication type, Integer userId,
                                         List<MultipartFile> newImages, List<String> imagesToKeep,
                                         List<MultipartFile> newPdfs, List<String> pdfsToKeep,
                                         String titleColor, String contentColor, String titleFontSize) throws IOException {

        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found"));

        if (!publication.getUserId().equals(userId))
            throw new RuntimeException("You are not authorized to modify this publication");

        if (titre    != null && !titre.trim().isEmpty())    publication.setTitre(titre);
        if (contenue != null && !contenue.trim().isEmpty()) publication.setContenue(contenue);
        if (type     != null) publication.setType(type);
        if (titleColor    != null) publication.setTitleColor(titleColor);
        if (contentColor  != null) publication.setContentColor(contentColor);
        if (titleFontSize != null) publication.setTitleFontSize(titleFontSize);

        // Images
        List<String> currentImages = new ArrayList<>(publication.getImages());
        for (String img : currentImages)
            if (imagesToKeep == null || !imagesToKeep.contains(img)) deleteFile(img);
        List<String> updatedImages = new ArrayList<>();
        if (imagesToKeep != null) updatedImages.addAll(imagesToKeep);
        if (newImages != null && !newImages.isEmpty())
            for (MultipartFile img : newImages)
                if (!img.isEmpty()) updatedImages.add(saveFile(img, false));
        publication.setImages(updatedImages);

        // PDFs
        List<String> currentPdfs = new ArrayList<>(publication.getPdfs());
        for (String pdf : currentPdfs)
            if (pdfsToKeep == null || !pdfsToKeep.contains(pdf)) deleteFile(pdf);
        List<String> updatedPdfs = new ArrayList<>();
        if (pdfsToKeep != null) updatedPdfs.addAll(pdfsToKeep);
        if (newPdfs != null && !newPdfs.isEmpty())
            for (MultipartFile pdf : newPdfs)
                if (!pdf.isEmpty()) updatedPdfs.add(saveFile(pdf, true));
        publication.setPdfs(updatedPdfs);

        Publication saved = publicationRepository.save(publication);
        enrichWithUser(saved);
        return saved;
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found"));
        if (!publication.getUserId().equals(userId))
            throw new RuntimeException("You are not authorized to delete this publication");
        for (String img : publication.getImages()) deleteFile(img);
        for (String pdf : publication.getPdfs()) deleteFile(pdf);
        publicationRepository.delete(publication);
    }

    public void adminDeletePublication(Integer id) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found"));
        for (String img : publication.getImages()) deleteFile(img);
        for (String pdf : publication.getPdfs()) deleteFile(pdf);
        publicationRepository.delete(publication);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void enrichWithUser(List<Publication> publications) {
        publications.forEach(this::enrichWithUser);
    }

    private void enrichWithUser(Publication p) {
        try {
            UserDTO dto = userClient.getUserById(p.getUserId());
            p.setUser(dto);
        } catch (Exception ignored) { }
    }

    private String saveFile(MultipartFile file, boolean isPdf) throws IOException {
        String contentType = file.getContentType();
        if (isPdf) {
            if (contentType == null || !contentType.equals("application/pdf"))
                throw new IllegalArgumentException("Invalid file: only PDFs are accepted");
        } else {
            if (contentType == null || !contentType.startsWith("image/"))
                throw new IllegalArgumentException("Invalid file: only images are accepted");
        }
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void deleteFile(String fileName) {
        try { Files.deleteIfExists(Paths.get(UPLOAD_DIR + fileName)); }
        catch (IOException e) { System.err.println("Error deleting file: " + e.getMessage()); }
    }
}
