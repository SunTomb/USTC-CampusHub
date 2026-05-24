ALTER TABLE project_ads ADD COLUMN ad_type VARCHAR(40) NOT NULL DEFAULT 'OTHER';
ALTER TABLE project_ads ADD COLUMN summary VARCHAR(500) NULL;
ALTER TABLE project_ads ADD COLUMN tags VARCHAR(500) NULL;
ALTER TABLE project_ads ADD COLUMN campus_zone VARCHAR(40) NULL;
ALTER TABLE project_ads ADD COLUMN cover_file_id BIGINT NULL;
ALTER TABLE project_ads ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'LOGIN_ONLY';
ALTER TABLE project_ads ADD COLUMN expires_at DATETIME NULL;
ALTER TABLE project_ads ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE project_ads ADD COLUMN featured_priority INT NOT NULL DEFAULT 0;
ALTER TABLE project_ads ADD COLUMN review_note VARCHAR(500) NULL;
ALTER TABLE project_ads ADD COLUMN reviewed_by BIGINT NULL;
ALTER TABLE project_ads ADD COLUMN reviewed_at DATETIME NULL;
ALTER TABLE project_ads ADD COLUMN published_at DATETIME NULL;
ALTER TABLE project_ads ADD COLUMN closed_at DATETIME NULL;

ALTER TABLE project_ads ADD CONSTRAINT fk_project_ads_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);
ALTER TABLE project_ads ADD CONSTRAINT fk_project_ads_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE INDEX idx_project_ads_public ON project_ads (status, featured, expires_at, created_at);
CREATE INDEX idx_project_ads_type_zone ON project_ads (ad_type, campus_zone);
CREATE INDEX idx_project_ads_publisher_status ON project_ads (publisher_id, status, created_at);
