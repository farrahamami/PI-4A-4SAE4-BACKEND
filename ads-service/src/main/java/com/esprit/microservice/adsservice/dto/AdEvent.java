package com.esprit.microservice.adsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdEvent {
    private Long adId;
    private EventType type;
    private CreatedBy createdBy;
    private Long userId;
    private LocalDateTime timestamp;

    public enum EventType {
        VIEW,
        CLICK,
        HOVER,
        CREATION
    }

    public enum CreatedBy {
        AI,
        HUMAN
    }
}
