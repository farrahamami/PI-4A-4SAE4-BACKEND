package com.esprit.forumservice.controllers;

import com.esprit.forumservice.entities.Commentaire;
import com.esprit.forumservice.services.CommentaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {

    private final CommentaireService commentaireService;

    @GetMapping
    public ResponseEntity<List<Commentaire>> getAll() {
        return ResponseEntity.ok(commentaireService.getAllCommentaires());
    }

    @GetMapping("/publication/{publicationId}")
    public ResponseEntity<List<Commentaire>> getByPublication(@PathVariable Integer publicationId) {
        return ResponseEntity.ok(commentaireService.getCommentairesByPublicationId(publicationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Commentaire> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(commentaireService.getCommentaireById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("contenue") String contenue,
            @RequestParam("publicationId") Integer publicationId,
            @RequestParam("userId") Integer userId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commentaireService.createCommentaire(contenue, publicationId, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la création");
        }
    }

    @PostMapping("/{parentId}/reply")
    public ResponseEntity<?> reply(
            @PathVariable Integer parentId,
            @RequestParam("contenue") String contenue,
            @RequestParam("publicationId") Integer publicationId,
            @RequestParam("userId") Integer userId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commentaireService.replyToCommentaire(contenue, parentId, publicationId, userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestParam("contenue") String contenue,
            @RequestParam("userId") Integer userId) {
        try {
            return ResponseEntity.ok(commentaireService.updateCommentaire(id, contenue, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, @RequestParam("userId") Integer userId) {
        try {
            commentaireService.deleteCommentaire(id, userId);
            return ResponseEntity.ok("Commentaire supprimé");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Integer id, @RequestParam("userId") Integer userId) {
        try {
            return ResponseEntity.ok(commentaireService.togglePin(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
