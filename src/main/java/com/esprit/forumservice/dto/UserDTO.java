package com.esprit.forumservice.dto;

import lombok.Data;

/**
 * Représentation locale d'un utilisateur dans le forum-service.
 * Pas de relation JPA : on résout l'utilisateur via Feign ou on le stocke en tant que projection.
 */
@Data
public class UserDTO {
    private Integer id;
    private String name;
    private String lastName;
}
