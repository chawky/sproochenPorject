package com.nailic.sproochencoach.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ProcessedStripeEvent {

    @Id
    private String eventId;

    private String eventType;

    private LocalDateTime processedAt;

    @Version
    private Long version;
}
