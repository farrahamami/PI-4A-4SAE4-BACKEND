package com.esprit.microservice.pidev.GestionForum.Services;

import com.esprit.microservice.pidev.Entities.User;
import com.esprit.microservice.pidev.GestionForum.Entities.Commentaire;
import com.esprit.microservice.pidev.GestionForum.Entities.Publication;
import com.esprit.microservice.pidev.GestionForum.Repositories.CommentaireRepository;
import com.esprit.microservice.pidev.GestionForum.Repositories.PublicationRepository;
import com.esprit.microservice.pidev.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;

    public List<Commentaire> getAllCommentaires() {
        return commentaireRepository.findAllByOrderByCreateAtDesc();
    }

    public List<Commentaire> getCommentairesByPublicationId(Integer publicationId) {
        return commentaireRepository.findByPublicationIdOrderByCreateAtAsc(publicationId);
    }

    public Commentaire getCommentaireById(Integer id) {
        return commentaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé avec l'id: " + id));
    }

    public Commentaire createCommentaire(String contenue, Integer publicationId, Integer userId) {

        System.out.println("═══════════════════════════════════════");
        System.out.println("📝 SERVICE - Création commentaire");
        System.out.println("Publication ID: " + publicationId);
        System.out.println("User ID: " + userId);
        System.out.println("Contenu: " + contenue);
        System.out.println("═══════════════════════════════════════");

        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ✅ Récupérer la publication COMPLÈTE (pas juste vérifier qu'elle existe)
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication non trouvée avec l'ID: " + publicationId));

        System.out.println("✅ User trouvé: " + user.getName());
        System.out.println("✅ Publication trouvée: " + publication.getTitre());

        Commentaire commentaire = new Commentaire();
        commentaire.setContenue(contenue);
        commentaire.setUser(user);
        commentaire.setPublication(publication);  // ✅ Assigner la PUBLICATION complète

        Commentaire saved = commentaireRepository.save(commentaire);

        System.out.println("✅ Commentaire créé avec ID: " + saved.getId());
        System.out.println("✅ Pour publication: " + saved.getPublication().getTitre());

        return saved;
    }

    public Commentaire updateCommentaire(Integer id, String contenue, Integer userId) {

        Commentaire commentaire = getCommentaireById(id);

        if (!commentaire.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier ce commentaire");
        }

        if (contenue != null && !contenue.trim().isEmpty()) {
            commentaire.setContenue(contenue);
        }

        return commentaireRepository.save(commentaire);
    }

    public void deleteCommentaire(Integer id, Integer userId) {
        Commentaire commentaire = getCommentaireById(id);

        if (!commentaire.getUser().getId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce commentaire");
        }

        commentaireRepository.delete(commentaire);
    }
}