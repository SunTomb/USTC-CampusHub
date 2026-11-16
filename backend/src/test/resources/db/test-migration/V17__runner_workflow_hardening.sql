-- Native runner workflow hardening: V17 only; V1-V16 remain immutable.
ALTER TABLE reward_tasks ADD COLUMN completion_code_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE reward_tasks ADD COLUMN completion_code_issued_at DATETIME(6) NULL;
