UPDATE report_records
SET status = 'OPEN'
WHERE status = 'PENDING';

ALTER TABLE report_records
    ADD COLUMN review_note VARCHAR(1000) NULL AFTER status,
    ADD COLUMN resolution_type VARCHAR(60) NULL AFTER review_note,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE violation_records
    ADD COLUMN severity VARCHAR(30) NOT NULL DEFAULT 'LOW' AFTER violation_type,
    ADD COLUMN penalty_type VARCHAR(40) NOT NULL DEFAULT 'CREDIT_ONLY' AFTER severity,
    ADD COLUMN target_type VARCHAR(40) NULL AFTER report_id,
    ADD COLUMN target_id BIGINT NULL AFTER target_type,
    ADD COLUMN admin_id BIGINT NULL AFTER credit_delta,
    ADD COLUMN deposit_impact_note VARCHAR(500) NULL AFTER admin_id,
    ADD CONSTRAINT fk_violation_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    ADD INDEX idx_violation_user_time (user_id, created_at),
    ADD INDEX idx_violation_target (target_type, target_id);

CREATE TABLE credit_adjustment_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    violation_id BIGINT NULL,
    before_score INT NOT NULL,
    delta_score INT NOT NULL,
    after_score INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credit_adjust_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_credit_adjust_violation FOREIGN KEY (violation_id) REFERENCES violation_records(id),
    CONSTRAINT fk_credit_adjust_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_credit_adjust_user_time (user_id, created_at)
);

CREATE TABLE user_restrictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    violation_id BIGINT NULL,
    restriction_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    starts_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at DATETIME NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    admin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_restriction_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_restriction_violation FOREIGN KEY (violation_id) REFERENCES violation_records(id),
    CONSTRAINT fk_user_restriction_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_user_restriction_user_active (user_id, active, restriction_type),
    INDEX idx_user_restriction_time (created_at)
);

CREATE TABLE admin_action_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NULL,
    action_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_action_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_admin_action_target (target_type, target_id),
    INDEX idx_admin_action_time (created_at)
);
