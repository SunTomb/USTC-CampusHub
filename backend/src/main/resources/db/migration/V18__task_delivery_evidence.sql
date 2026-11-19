-- Task weight/notes remain nullable for legacy rows; new task requests validate them in the service/API layer.
ALTER TABLE reward_tasks ADD COLUMN weight DECIMAL(10,3) NULL;
ALTER TABLE reward_tasks ADD COLUMN public_notes TEXT NULL;
ALTER TABLE reward_tasks ADD COLUMN pickup_code VARCHAR(128) NULL;
ALTER TABLE reward_tasks ADD COLUMN private_pickup_notes TEXT NULL;

CREATE TABLE task_delivery_photos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    accepted_application_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    note VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_delivery_photo_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_delivery_photo_application FOREIGN KEY (accepted_application_id) REFERENCES task_applications(id),
    CONSTRAINT fk_task_delivery_photo_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT uq_task_delivery_photo_request UNIQUE (task_id, uploader_id, client_request_id),
    INDEX idx_task_delivery_photo_task_status (task_id, status, created_at)
);
