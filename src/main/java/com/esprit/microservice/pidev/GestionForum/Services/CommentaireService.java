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

    // ✅ Retourne uniquement les commentaires racine (sans parent) avec leurs replies chargées
    public List<Commentaire> getCommentairesByPublicationId(Integer publicationId) {
        return commentaireRepository.findRootCommentairesByPublicationId(publicationId);
    }

    public Commentaire getCommentaireById(Integer id) {
        return commentaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé avec l'id: " + id));
    }

    // ✅ Créer un commentaire racine (sans parent)
    public Commentaire createCommentaire(String contenue, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication non trouvée avec l'ID: " + publicationId));

        Commentaire commentaire = new Commentaire();
        commentaire.setContenue(contenue);
        commentaire.setUser(user);
        commentaire.setPublication(publication);
        commentaire.setParent(null); // commentaire racine

        return commentaireRepository.save(commentaire);
    }

    // ✅ NOUVEAU : Créer une réponse à un commentaire existant
    public Commentaire replyToCommentaire(String contenue, Integer parentId, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu de la réponse est obligatoire");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication non trouvée avec l'ID: " + publicationId));

        Commentaire parent = commentaireRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Commentaire parent non trouvé avec l'ID: " + parentId));

        Commentaire reply = new Commentaire();
        reply.setContenue(contenue);
        reply.setUser(user);
        reply.setPublication(publication);
        reply.setParent(parent);

        return commentaireRepository.save(reply);
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