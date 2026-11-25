CREATE TABLE catalog_images (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 kind VARCHAR(20) NOT NULL, object_id BIGINT NOT NULL, uploader_id BIGINT NOT NULL,
 client_key VARCHAR(80) NOT NULL, sha256 CHAR(64) NOT NULL,
 mime_type VARCHAR(20) NOT NULL, payload MEDIUMBLOB NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
 CONSTRAINT fk_catalog_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
 CONSTRAINT uk_catalog_client UNIQUE (kind,object_id,client_key)
);
CREATE INDEX idx_catalog_parent ON catalog_images(kind,object_id,id);
