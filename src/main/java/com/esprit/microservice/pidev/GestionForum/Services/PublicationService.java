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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/publications/";

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

    public Publication createPublication(String titre, String contenue, TypePublication type, 
                                         Integer userId, MultipartFile image) throws IOException {
        
        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu est obligatoire");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type est obligatoire");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Publication publication = new Publication();
        publication.setTitre(titre);
        publication.setContenue(contenue);
        publication.setType(type);
        publication.setUser(user);

        if (image != null && !image.isEmpty()) {
            String imagePath = saveImage(image);
            publication.setImage(imagePath);
        }

        return publicationRepository.save(publication);
    }

    public Publication updatePublication(Integer id, String titre, String contenue, 
                                         TypePublication type, Integer userId, MultipartFile image) throws IOException {
        
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

        if (image != null && !image.isEmpty()) {
            // Supprimer l'ancienne image si elle existe
            if (publication.getImage() != null) {
                deleteImage(publication.getImage());
            }
            String imagePath = saveImage(image);
            publication.setImage(imagePath);
        }

        return publicationRepository.save(publication);
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication publication = getPublicationById(id);

        if (!publication.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette publication");
        }

        if (publication.getImage() != null) {
            deleteImage(publication.getImage());
        }

        publicationRepository.delete(publication);
    }

    private String saveImage(MultipartFile image) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    private void deleteImage(String imagePath) {
        try {
            Path path = Paths.get(UPLOAD_DIR + imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression de l'image: " + e.getMessage());
        }
    }
}
