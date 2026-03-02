package com.esprit.forumservice.services;

import com.esprit.forumservice.clients.UserClient;
import com.esprit.forumservice.dto.UserDTO;
import com.esprit.forumservice.entities.Commentaire;
import com.esprit.forumservice.entities.Publication;
import com.esprit.forumservice.repositories.CommentaireRepository;
import com.esprit.forumservice.repositories.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final PublicationRepository publicationRepository;
    private final UserClient userClient;

    public List<Commentaire> getAllCommentaires() {
        List<Commentaire> list = commentaireRepository.findAllByOrderByCreateAtDesc();
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Commentaire> getCommentairesByPublicationId(Integer publicationId) {
        List<Commentaire> list = commentaireRepository.findRootCommentairesByPublicationIdOrderByPinned(publicationId);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public Commentaire getCommentaireById(Integer id) {
        return commentaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé: " + id));
    }

    public Commentaire createCommentaire(String contenue, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty())
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire");

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication non trouvée: " + publicationId));

        Commentaire commentaire = new Commentaire();
        commentaire.setContenue(contenue);
        commentaire.setUserId(userId);
        commentaire.setPublication(publication);

        Commentaire saved = commentaireRepository.save(commentaire);
        enrichWithUser(saved);
        return saved;
    }

    public Commentaire replyToCommentaire(String contenue, Integer parentId, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty())
            throw new IllegalArgumentException("Le contenu de la réponse est obligatoire");

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication non trouvée: " + publicationId));

        Commentaire parent = commentaireRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Commentaire parent non trouvé: " + parentId));

        Commentaire reply = new Commentaire();
        reply.setContenue(contenue);
        reply.setUserId(userId);
        reply.setPublication(publication);
        reply.setParent(parent);

        Commentaire saved = commentaireRepository.save(reply);
        enrichWithUser(saved);
        return saved;
    }

    public Commentaire updateCommentaire(Integer id, String contenue, Integer userId) {
        Commentaire commentaire = getCommentaireById(id);
        if (!commentaire.getUserId().equals(userId))
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier ce commentaire");
        if (contenue != null && !contenue.trim().isEmpty())
            commentaire.setContenue(contenue);
        return commentaireRepository.save(commentaire);
    }

    public void deleteCommentaire(Integer id, Integer userId) {
        Commentaire commentaire = getCommentaireById(id);
        if (!commentaire.getUserId().equals(userId))
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce commentaire");
        commentaireRepository.delete(commentaire);
    }

    public Commentaire togglePin(Integer commentaireId, Integer userId) {
        Commentaire commentaire = getCommentaireById(commentaireId);
        Integer publicationOwnerId = commentaire.getPublication().getUserId();
        if (!publicationOwnerId.equals(userId))
            throw new RuntimeException("Accès refusé : seul le créateur de la publication peut épingler");
        commentaire.setPinned(!commentaire.isPinned());
        return commentaireRepository.save(commentaire);
    }

    private void enrichWithUser(Commentaire c) {
        try {
            UserDTO dto = userClient.getUserById(c.getUserId());
            c.setUser(dto);
        } catch (Exception ignored) { }
    }
}
