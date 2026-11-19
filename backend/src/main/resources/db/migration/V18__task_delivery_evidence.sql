-- Task weight/notes remain nullable for legacy rows; new task requests validate them in the service/API layer.
ALTER TABLE reward_tasks ADD COLUMN weight_grams INT NULL CHECK (weight_grams BETWEEN 1 AND 100000);
ALTER TABLE reward_tasks ADD COLUMN notes VARCHAR(1000) NULL;
ALTER TABLE reward_tasks ADD COLUMN pickup_code VARCHAR(120) NULL;

CREATE TABLE task_delivery_photos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    accepted_application_id BIGINT NULL,
    evidence_type VARCHAR(30) NOT NULL,
    uploader_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    note VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_delivery_photo_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_delivery_photo_application FOREIGN KEY (accepted_application_id) REFERENCES task_applications(id),
    CONSTRAINT fk_task_delivery_photo_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT uq_task_delivery_photo_request UNIQUE (task_id, evidence_type, uploader_id, client_request_id),
    CONSTRAINT uq_task_photo_storage UNIQUE (storage_key),
    CHECK ((evidence_type = 'REQUEST_IMAGE' AND accepted_application_id IS NULL) OR
           (evidence_type = 'DELIVERY_PHOTO' AND accepted_application_id IS NOT NULL)),
    INDEX idx_task_photo_type_status (task_id, evidence_type, status)
);
