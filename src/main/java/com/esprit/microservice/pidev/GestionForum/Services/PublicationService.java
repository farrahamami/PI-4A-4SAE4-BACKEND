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
    // ✅ Nombre maximum d'images autorisées par publication
    private static final int MAX_IMAGES = 5;

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
                .orElseThrow(() -> new RuntimeException("Publication non trouvée avec l'id: " + id));
    }

    // ✅ CREATION avec plusieurs images
    public Publication createPublication(String titre, String contenue, TypePublication type,
                                         Integer userId, List<MultipartFile> images) throws IOException {

        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu est obligatoire");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type est obligatoire");
        }
        if (images != null && images.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Vous ne pouvez pas uploader plus de " + MAX_IMAGES + " images");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Publication publication = new Publication();
        publication.setTitre(titre);
        publication.setContenue(contenue);
        publication.setType(type);
        publication.setUser(user);

        // ✅ Sauvegarder chaque image
        if (images != null && !images.isEmpty()) {
            List<String> imageNames = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageName = saveImage(image);
                    imageNames.add(imageName);
                }
            }
            publication.setImages(imageNames);
        }

        return publicationRepository.save(publication);
    }

    // ✅ MISE A JOUR avec plusieurs images
    public Publication updatePublication(Integer id, String titre, String contenue,
                                         TypePublication type, Integer userId,
                                         List<MultipartFile> newImages,
                                         List<String> imagesToKeep) throws IOException {

        Publication publication = getPublicationById(id);

        if (!publication.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette publication");
        }

        if (titre != null && !titre.trim().isEmpty()) {
            publication.setTitre(titre);
        }
        if (contenue != null && !contenue.trim().isEmpty()) {
            publication.setContenue(contenue);
        }
        if (type != null) {
            publication.setType(type);
        }

        // ✅ Supprimer les images qui ne sont plus dans la liste "à garder"
        List<String> currentImages = new ArrayList<>(publication.getImages());
        for (String currentImage : currentImages) {
            if (imagesToKeep == null || !imagesToKeep.contains(currentImage)) {
                deleteImage(currentImage);
            }
        }

        // ✅ Construire la nouvelle liste : images conservées + nouvelles
        List<String> updatedImages = new ArrayList<>();
        if (imagesToKeep != null) {
            updatedImages.addAll(imagesToKeep);
        }

        // Ajouter les nouvelles images uploadées
        if (newImages != null && !newImages.isEmpty()) {
            int totalImages = updatedImages.size() + newImages.size();
            if (totalImages > MAX_IMAGES) {
                throw new IllegalArgumentException("Le total d'images ne peut pas dépasser " + MAX_IMAGES);
            }
            for (MultipartFile image : newImages) {
                if (!image.isEmpty()) {
                    String imageName = saveImage(image);
                    updatedImages.add(imageName);
                }
            }
        }

        publication.setImages(updatedImages);
        return publicationRepository.save(publication);
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication publication = getPublicationById(id);

        if (!publication.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette publication");
        }

        // ✅ Supprimer toutes les images associées
        for (String imageName : publication.getImages()) {
            deleteImage(imageName);
        }

        publicationRepository.delete(publication);
    }

    private String saveImage(MultipartFile image) throws IOException {
        // Validation du type MIME
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Fichier invalide : seules les images sont acceptées");
        }
        // Validation taille (5 MB)
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("L'image ne doit pas dépasser 5 MB");
        }

        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    private void deleteImage(String imageName) {
        try {
            Path path = Paths.get(UPLOAD_DIR + imageName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression de l'image: " + e.getMessage());
        }
    }
}
