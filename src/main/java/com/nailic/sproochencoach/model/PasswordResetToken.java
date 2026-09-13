package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@RequiredArgsConstructor
@Data
public class PasswordResetToken extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String codeHash;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private LocalDateTime resetCreationDate;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;
}
