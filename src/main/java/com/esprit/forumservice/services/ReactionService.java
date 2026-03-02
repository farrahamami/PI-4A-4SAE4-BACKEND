package com.esprit.forumservice.services;

import com.esprit.forumservice.clients.UserClient;
import com.esprit.forumservice.dto.ReactionSummaryDTO;
import com.esprit.forumservice.dto.ReactorDTO;
import com.esprit.forumservice.dto.UserDTO;
import com.esprit.forumservice.entities.Publication;
import com.esprit.forumservice.entities.Reaction;
import com.esprit.forumservice.entities.TypeReaction;
import com.esprit.forumservice.repositories.PublicationRepository;
import com.esprit.forumservice.repositories.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PublicationRepository publicationRepository;
    private final UserClient userClient;

    @Transactional
    public Optional<Reaction> toggleReaction(Integer publicationId, Integer userId, TypeReaction type) {
        // Validate user exists
        try { userClient.getUserById(userId); }
        catch (Exception e) { throw new RuntimeException("User not found: " + userId); }

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new RuntimeException("Publication not found"));

        Optional<Reaction> existing = reactionRepository.findByPublicationIdAndUserId(publicationId, userId);

        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            if (reaction.getType() == type) {
                reactionRepository.delete(reaction);
                return Optional.empty();
            } else {
                reaction.setType(type);
                return Optional.of(reactionRepository.save(reaction));
            }
        } else {
            Reaction reaction = new Reaction();
            reaction.setUserId(userId);
            reaction.setPublication(publication);
            reaction.setType(type);
            return Optional.of(reactionRepository.save(reaction));
        }
    }

    public ReactionSummaryDTO getSummary(Integer publicationId, Integer userId) {
        List<Reaction> all = reactionRepository.findByPublicationId(publicationId);

        long likes    = all.stream().filter(r -> r.getType() == TypeReaction.LIKE).count();
        long dislikes = all.stream().filter(r -> r.getType() == TypeReaction.DISLIKE).count();
        long hearts   = all.stream().filter(r -> r.getType() == TypeReaction.HEART).count();

        TypeReaction userReaction = all.stream()
                .filter(r -> r.getUserId().equals(userId))
                .map(Reaction::getType)
                .findFirst().orElse(null);

        List<ReactorDTO> reactors = all.stream().map(r -> {
            String name = "User " + r.getUserId();
            try {
                UserDTO dto = userClient.getUserById(r.getUserId());
                name = dto.getName() + " " + dto.getLastName();
            } catch (Exception ignored) { }
            return new ReactorDTO(r.getUserId(), name, r.getType());
        }).collect(Collectors.toList());

        return new ReactionSummaryDTO(likes, dislikes, hearts, userReaction, reactors);
    }
}
