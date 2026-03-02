package com.esprit.forumservice.repositories;

import com.esprit.forumservice.entities.Reaction;
import com.esprit.forumservice.entities.TypeReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Integer> {

    List<Reaction> findByPublicationId(Integer publicationId);

    Optional<Reaction> findByPublicationIdAndUserId(Integer publicationId, Integer userId);

    long countByPublicationIdAndType(Integer publicationId, TypeReaction type);
}
