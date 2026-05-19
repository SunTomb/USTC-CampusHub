CREATE TABLE email_verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    attempt_count INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_email_codes_lookup (email, purpose, created_at),
    INDEX idx_email_codes_expires (expires_at),
    INDEX idx_email_codes_used (used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
