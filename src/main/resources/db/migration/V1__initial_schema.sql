CREATE TABLE app_role (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE subscription_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    payment_status VARCHAR(255),
    subscription_status VARCHAR(255),
    started_at DATE,
    current_period_end DATE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE app_users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255),
    password VARCHAR(255),
    email VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    street VARCHAR(255),
    street_number VARCHAR(255),
    postal_code VARCHAR(255),
    city VARCHAR(255),
    address_info VARCHAR(255),
    enabled BIT NOT NULL,
    admin_disabled BIT NOT NULL,
    token_version INT NOT NULL DEFAULT 0,
    subscription_plan_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_subscription_plan_id (subscription_plan_id),
    CONSTRAINT fk_app_users_subscription_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plan (id)
) ENGINE=InnoDB;

CREATE TABLE app_user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_app_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_app_user_roles_role FOREIGN KEY (role_id) REFERENCES app_role (id)
) ENGINE=InnoDB;

CREATE TABLE otp (
    id BIGINT NOT NULL AUTO_INCREMENT,
    otp INT,
    attempts INT,
    otp_creation_date DATETIME(6),
    user_id INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_otp_user_id (user_id),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE password_reset_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL,
    reset_creation_date DATETIME(6) NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_token_user_id (user_id),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE exercise_levels (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    enabled BIT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_levels_code (code)
) ENGINE=InnoDB;

CREATE TABLE exercise_topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    level_code VARCHAR(255) NOT NULL,
    enabled BIT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_topics_code (code)
) ENGINE=InnoDB;

CREATE TABLE exercise_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    enabled BIT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_types_code (code)
) ENGINE=InnoDB;

CREATE TABLE exercise_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    exercise_type VARCHAR(255) NOT NULL,
    exercise_name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    level VARCHAR(255),
    topic VARCHAR(255),
    answer_type VARCHAR(255),
    learner_answer VARCHAR(2000),
    average_rating_overall DOUBLE,
    generated_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    evaluated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_exercise_attempts_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE user_progress (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exercise_type VARCHAR(255) NOT NULL,
    exercise_name VARCHAR(255) NOT NULL,
    average_rating_overall DOUBLE,
    user_id INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE user_login_days (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_date DATE NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_login_days_user_date (user_id, login_date),
    CONSTRAINT fk_user_login_days_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE ai_usage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    request_name VARCHAR(255) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    total_tokens INT,
    usage_unit VARCHAR(255),
    usage_amount BIGINT,
    estimated_cost_usd DECIMAL(12, 6),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE outbound_api_call_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(80) NOT NULL,
    method VARCHAR(10) NOT NULL,
    uri VARCHAR(1000) NOT NULL,
    status_code INT,
    duration_ms BIGINT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    error_type VARCHAR(200),
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE prompt_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prompt_key VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    editable_content VARCHAR(3000),
    enabled BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prompt_templates_prompt_key (prompt_key)
) ENGINE=InnoDB;

CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id INT NOT NULL,
    target_user_id INT,
    target_type VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    old_value VARCHAR(1000) NOT NULL,
    new_value VARCHAR(1000) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE processed_stripe_event (
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255),
    processed_at DATETIME(6),
    version BIGINT,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB;
