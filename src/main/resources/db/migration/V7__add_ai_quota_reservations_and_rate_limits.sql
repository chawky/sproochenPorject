CREATE TABLE ai_feature_quota_reservations (
    id VARCHAR(36) NOT NULL,
    user_id INT NOT NULL,
    feature VARCHAR(64) NOT NULL,
    window_start DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_quota_reservation_lookup (user_id, feature, window_start),
    CONSTRAINT fk_ai_quota_reservation_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE ai_rate_limit_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_rate_limit_lookup (user_id, created_at),
    CONSTRAINT fk_ai_rate_limit_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;
