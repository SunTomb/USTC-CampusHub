ALTER TABLE users
    ADD COLUMN wechat_contact VARCHAR(120) NULL AFTER email,
    ADD COLUMN qq_contact VARCHAR(60) NULL AFTER wechat_contact;

CREATE TABLE role_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL,
    deposit_status VARCHAR(30) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    apply_note VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_app_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_app_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT uk_role_app_user_type UNIQUE (user_id, role_type)
);
