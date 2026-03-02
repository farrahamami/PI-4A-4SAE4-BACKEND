package com.esprit.forumservice.controllers;

import com.esprit.forumservice.entities.Reaction;
import com.esprit.forumservice.entities.TypeReaction;
import com.esprit.forumservice.dto.ReactionSummaryDTO;
import com.esprit.forumservice.services.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/publication/{publicationId}")
    public ResponseEntity<?> toggleReaction(
            @PathVariable Integer publicationId,
            @RequestParam Integer userId,
            @RequestParam TypeReaction type) {
        Optional<Reaction> result = reactionService.toggleReaction(publicationId, userId, type);
        return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/publication/{publicationId}/summary")
    public ResponseEntity<ReactionSummaryDTO> getSummary(
            @PathVariable Integer publicationId,
            @RequestParam Integer userId) {
        return ResponseEntity.ok(reactionService.getSummary(publicationId, userId));
    }
}
