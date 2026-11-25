ALTER TABLE report_records ADD COLUMN order_dedupe_key VARCHAR(100) NULL;
CREATE UNIQUE INDEX uk_report_order_dedupe ON report_records(order_dedupe_key);
ALTER TABLE reviews ADD COLUMN rated_item_id BIGINT NULL;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_rated_item FOREIGN KEY (rated_item_id) REFERENCES service_items(id);
CREATE INDEX idx_reviews_rated_item ON reviews(rated_item_id, created_at);
