CREATE TABLE support_emails (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resend_email_id VARCHAR(255) NOT NULL,
    from_email VARCHAR(255) NOT NULL,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    text_body TEXT,
    html_body TEXT,
    received_at DATETIME(6) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_emails_resend_email_id (resend_email_id)
) ENGINE=InnoDB;
