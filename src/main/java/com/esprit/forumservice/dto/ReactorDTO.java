package com.esprit.forumservice.dto;

import com.esprit.forumservice.entities.TypeReaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class ReactorDTO {
    @JsonProperty("userId")   private Integer userId;
    @JsonProperty("userName") private String userName;
    @JsonProperty("type")     private TypeReaction type;
}
