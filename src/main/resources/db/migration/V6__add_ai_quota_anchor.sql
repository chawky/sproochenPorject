ALTER TABLE app_users
    ADD COLUMN ai_quota_anchor DATE NULL;

UPDATE app_users
SET ai_quota_anchor = CURRENT_DATE
WHERE ai_quota_anchor IS NULL;

ALTER TABLE app_users
    MODIFY ai_quota_anchor DATE NOT NULL DEFAULT (CURRENT_DATE);
