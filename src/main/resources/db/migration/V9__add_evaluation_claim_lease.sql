ALTER TABLE exercise_attempts
    ADD COLUMN evaluation_claimed_at DATETIME(6) NULL AFTER evaluated_at;
