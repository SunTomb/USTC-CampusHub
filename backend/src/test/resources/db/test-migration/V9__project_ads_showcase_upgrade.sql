ALTER TABLE project_ads
    ADD COLUMN ad_type VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN summary VARCHAR(500) NULL,
    ADD COLUMN tags VARCHAR(500) NULL,
    ADD COLUMN campus_zone VARCHAR(40) NULL,
    ADD COLUMN cover_file_id BIGINT NULL,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'LOGIN_ONLY',
    ADD COLUMN expires_at DATETIME NULL,
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN featured_priority INT NOT NULL DEFAULT 0,
    ADD COLUMN review_note VARCHAR(500) NULL,
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN published_at DATETIME NULL,
    ADD COLUMN closed_at DATETIME NULL;

ALTER TABLE project_ads
    ADD CONSTRAINT fk_project_ads_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id),
    ADD CONSTRAINT fk_project_ads_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE INDEX idx_project_ads_public ON project_ads (status, featured, expires_at, created_at);
CREATE INDEX idx_project_ads_type_zone ON project_ads (ad_type, campus_zone);
CREATE INDEX idx_project_ads_publisher_status ON project_ads (publisher_id, status, created_at);
