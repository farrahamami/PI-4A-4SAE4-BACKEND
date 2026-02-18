package com.esprit.microservice.pidev.GestionForum.Services;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.GestionForum.Entities.Publication;
import com.esprit.microservice.pidev.GestionForum.Entities.TypePublication;
import com.esprit.microservice.pidev.GestionForum.Repositories.PublicationRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/publications/";
    private static final int MAX_IMAGES = 5;
    private static final int MAX_PDFS = 5;

    public List<Publication> getAllPublications() {
        return publicationRepository.findAllByOrderByCreateAtDesc();
    }

    public List<Publication> getPublicationsByType(TypePublication type) {
        return publicationRepository.findByType(type);
    }

    public List<Publication> getPublicationsByUserId(Integer userId) {
        return publicationRepository.findByUserId(userId);
    }

    public Publication getPublicationById(Integer id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found with id: " + id));
    }

    // ✅ CREATE with images + PDFs
    public Publication createPublication(String titre, String contenue, TypePublication type,
                                         Integer userId, List<MultipartFile> images,
                                         List<MultipartFile> pdfs) throws IOException {

        if (titre == null || titre.trim().isEmpty())
            throw new IllegalArgumentException("Title is required");
        if (contenue == null || contenue.trim().isEmpty())
            throw new IllegalArgumentException("Content is required");
        if (type == null)
            throw new IllegalArgumentException("Type is required");
        if (images != null && images.size() > MAX_IMAGES)
            throw new IllegalArgumentException("Cannot upload more than " + MAX_IMAGES + " images");
        if (pdfs != null && pdfs.size() > MAX_PDFS)
            throw new IllegalArgumentException("Cannot upload more than " + MAX_PDFS + " PDFs");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Publication publication = new Publication();
        publication.setTitre(titre);
        publication.setContenue(contenue);
        publication.setType(type);
        publication.setUser(user);

        // Save images
        if (images != null && !images.isEmpty()) {
            List<String> imageNames = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) imageNames.add(saveFile(image, false));
            }
            publication.setImages(imageNames);
        }

        // Save PDFs
        if (pdfs != null && !pdfs.isEmpty()) {
            List<String> pdfNames = new ArrayList<>();
            for (MultipartFile pdf : pdfs) {
                if (!pdf.isEmpty()) pdfNames.add(saveFile(pdf, true));
            }
            publication.setPdfs(pdfNames);
        }

        return publicationRepository.save(publication);
    }

    // ✅ UPDATE with images + PDFs
    public Publication updatePublication(Integer id, String titre, String contenue,
                                         TypePublication type, Integer userId,
                                         List<MultipartFile> newImages, List<String> imagesToKeep,
                                         List<MultipartFile> newPdfs, List<String> pdfsToKeep) throws IOException {

        Publication publication = getPublicationById(id);

        if (!publication.getUser().getId().equals(userId))
            throw new RuntimeException("You are not authorized to modify this publication");

        if (titre != null && !titre.trim().isEmpty()) publication.setTitre(titre);
        if (contenue != null && !contenue.trim().isEmpty()) publication.setContenue(contenue);
        if (type != null) publication.setType(type);

        // ── Images ──
        List<String> currentImages = new ArrayList<>(publication.getImages());
        for (String img : currentImages) {
            if (imagesToKeep == null || !imagesToKeep.contains(img)) deleteFile(img);
        }
        List<String> updatedImages = new ArrayList<>();
        if (imagesToKeep != null) updatedImages.addAll(imagesToKeep);
        if (newImages != null && !newImages.isEmpty()) {
            if (updatedImages.size() + newImages.size() > MAX_IMAGES)
                throw new IllegalArgumentException("Total images cannot exceed " + MAX_IMAGES);
            for (MultipartFile img : newImages) {
                if (!img.isEmpty()) updatedImages.add(saveFile(img, false));
            }
        }
        publication.setImages(updatedImages);

        // ── PDFs ──
        List<String> currentPdfs = new ArrayList<>(publication.getPdfs());
        for (String pdf : currentPdfs) {
            if (pdfsToKeep == null || !pdfsToKeep.contains(pdf)) deleteFile(pdf);
        }
        List<String> updatedPdfs = new ArrayList<>();
        if (pdfsToKeep != null) updatedPdfs.addAll(pdfsToKeep);
        if (newPdfs != null && !newPdfs.isEmpty()) {
            if (updatedPdfs.size() + newPdfs.size() > MAX_PDFS)
                throw new IllegalArgumentException("Total PDFs cannot exceed " + MAX_PDFS);
            for (MultipartFile pdf : newPdfs) {
                if (!pdf.isEmpty()) updatedPdfs.add(saveFile(pdf, true));
            }
        }
        publication.setPdfs(updatedPdfs);

        return publicationRepository.save(publication);
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication publication = getPublicationById(id);

        if (!publication.getUser().getId().equals(userId))
            throw new RuntimeException("You are not authorized to delete this publication");

        for (String img : publication.getImages()) deleteFile(img);
        for (String pdf : publication.getPdfs()) deleteFile(pdf);

        publicationRepository.delete(publication);
    }


    // ✅ ADMIN DELETE — bypasses userId check
    public void adminDeletePublication(Integer id) {
        Publication publication = getPublicationById(id);
        for (String img : publication.getImages()) deleteFile(img);
        for (String pdf : publication.getPdfs()) deleteFile(pdf);
        publicationRepository.delete(publication);
    }


    // ✅ Unified save: images and PDFs go to the same folder
    private String saveFile(MultipartFile file, boolean isPdf) throws IOException {
        String contentType = file.getContentType();
        if (isPdf) {
            if (contentType == null || !contentType.equals("application/pdf"))
                throw new IllegalArgumentException("Invalid file: only PDFs are accepted");
            if (file.getSize() > 20 * 1024 * 1024)
                throw new IllegalArgumentException("PDF must not exceed 20 MB");
        } else {
            if (contentType == null || !contentType.startsWith("image/"))
                throw new IllegalArgumentException("Invalid file: only images are accepted");
            if (file.getSize() > 5 * 1024 * 1024)
                throw new IllegalArgumentException("Image must not exceed 5 MB");
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(UPLOAD_DIR + fileName));
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
    }
}