package com.esprit.microservice.pidev.GestionForum.Controllers;

import com.esprit.microservice.pidev.GestionForum.Entities.Commentaire;
import com.esprit.microservice.pidev.GestionForum.Services.CommentaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CommentaireController {

    private final CommentaireService commentaireService;

    @GetMapping
    public ResponseEntity<List<Commentaire>> getAllCommentaires() {
        try {
            return ResponseEntity.ok(commentaireService.getAllCommentaires());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/publication/{publicationId}")
    public ResponseEntity<List<Commentaire>> getCommentairesByPublicationId(@PathVariable Integer publicationId) {
        try {
            return ResponseEntity.ok(commentaireService.getCommentairesByPublicationId(publicationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Commentaire> getCommentaireById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(commentaireService.getCommentaireById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createCommentaire(
            @RequestParam("contenue") String contenue,
            @RequestParam("publicationId") Integer publicationId,
            @RequestParam("userId") Integer userId) {
        try {
            Commentaire commentaire = commentaireService.createCommentaire(contenue, publicationId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(commentaire);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du commentaire");
        }
    }

    // ✅ NOUVEAU endpoint : répondre à un commentaire
    @PostMapping("/{parentId}/reply")
    public ResponseEntity<?> replyToCommentaire(
            @PathVariable Integer parentId,
            @RequestParam("contenue") String contenue,
            @RequestParam("publicationId") Integer publicationId,
            @RequestParam("userId") Integer userId) {
        try {
            Commentaire reply = commentaireService.replyToCommentaire(contenue, parentId, publicationId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reply);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création de la réponse");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCommentaire(
            @PathVariable Integer id,
            @RequestParam("contenue") String contenue,
            @RequestParam("userId") Integer userId) {
        try {
            Commentaire commentaire = commentaireService.updateCommentaire(id, contenue, userId);
            return ResponseEntity.ok(commentaire);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la modification du commentaire");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCommentaire(@PathVariable Integer id, @RequestParam("userId") Integer userId) {
        try {
            commentaireService.deleteCommentaire(id, userId);
            return ResponseEntity.ok("Commentaire supprimé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du commentaire");
        }
    }
}