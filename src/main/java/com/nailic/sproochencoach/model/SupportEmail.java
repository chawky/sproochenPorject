package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "support_emails",
        uniqueConstraints = @UniqueConstraint(name = "uk_support_emails_resend_email_id", columnNames = "resend_email_id")
)
@Getter
@Setter
@NoArgsConstructor
public class SupportEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resend_email_id", nullable = false)
    private String resendEmailId;

    @Column(nullable = false)
    private String fromEmail;

    @Column(nullable = false)
    private String toEmail;

    @Column
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String textBody;

    @Column(columnDefinition = "TEXT")
    private String htmlBody;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
