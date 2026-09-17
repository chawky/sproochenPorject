ALTER TABLE subscription_plan
    ADD COLUMN cancel_at_period_end BIT NOT NULL DEFAULT 0;
