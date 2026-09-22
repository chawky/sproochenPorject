package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "support_email_attachments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_support_email_attachments_email_resend_attachment",
                columnNames = {"support_email_id", "resend_attachment_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class SupportEmailAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_email_id", nullable = false)
    private SupportEmail supportEmail;

    @Column(name = "resend_attachment_id", nullable = false)
    private String resendAttachmentId;

    @Column
    private String filename;

    @Column
    private String contentType;

    @Column
    private String contentDisposition;

    @Column
    private String contentId;

    @Column
    private Long sizeBytes;
}
