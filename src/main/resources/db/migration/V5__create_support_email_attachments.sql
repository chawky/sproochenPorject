CREATE TABLE support_email_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    support_email_id BIGINT NOT NULL,
    resend_attachment_id VARCHAR(255) NOT NULL,
    filename VARCHAR(255),
    content_type VARCHAR(255),
    content_disposition VARCHAR(255),
    content_id VARCHAR(255),
    size_bytes BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_email_attachments_email_resend_attachment (support_email_id, resend_attachment_id),
    CONSTRAINT fk_support_email_attachments_support_email
        FOREIGN KEY (support_email_id) REFERENCES support_emails (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
