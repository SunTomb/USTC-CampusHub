ALTER TABLE reward_tasks
    ADD COLUMN acceptance_mode VARCHAR(30) NOT NULL DEFAULT 'GRAB',
    ADD COLUMN origin_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN destination_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN origin_detail VARCHAR(255) NULL,
    ADD COLUMN destination_detail VARCHAR(255) NULL,
    ADD COLUMN workflow_status VARCHAR(40) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN verification_mode VARCHAR(40) NOT NULL DEFAULT 'COMPLETION_CODE',
    ADD COLUMN completion_code_hash VARCHAR(120) NULL,
    ADD COLUMN accepted_application_id BIGINT NULL;

CREATE TABLE task_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    event_type VARCHAR(60) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_event_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_event_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_task_event_task_time (task_id, created_at)
);

CREATE TABLE task_issues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    issue_type VARCHAR(60) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    handler_id BIGINT NULL,
    handled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_issue_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_issue_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_task_issue_handler FOREIGN KEY (handler_id) REFERENCES users(id),
    INDEX idx_task_issue_status_time (status, created_at)
);
