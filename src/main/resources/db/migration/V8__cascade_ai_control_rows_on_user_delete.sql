ALTER TABLE ai_feature_quota_reservations
    DROP FOREIGN KEY fk_ai_quota_reservation_user;

ALTER TABLE ai_feature_quota_reservations
    ADD CONSTRAINT fk_ai_quota_reservation_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE;

ALTER TABLE ai_rate_limit_requests
    DROP FOREIGN KEY fk_ai_rate_limit_user;

ALTER TABLE ai_rate_limit_requests
    ADD CONSTRAINT fk_ai_rate_limit_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE;
