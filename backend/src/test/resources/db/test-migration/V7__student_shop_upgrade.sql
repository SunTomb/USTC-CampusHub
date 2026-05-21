ALTER TABLE shops ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER';
ALTER TABLE shops ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'ORDER_ONLY';
ALTER TABLE shops ADD COLUMN opening_hours VARCHAR(255) NULL;
ALTER TABLE shops ADD COLUMN cover_file_id BIGINT NULL;
ALTER TABLE shops ADD CONSTRAINT fk_shop_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_items ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'OTHER';
ALTER TABLE service_items ADD COLUMN min_price DECIMAL(10,2) NULL;
ALTER TABLE service_items ADD COLUMN max_price DECIMAL(10,2) NULL;
ALTER TABLE service_items ADD COLUMN price_unit VARCHAR(30) NOT NULL DEFAULT '次';
ALTER TABLE service_items ADD COLUMN cover_file_id BIGINT NULL;
ALTER TABLE service_items ADD CONSTRAINT fk_service_item_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_orders ADD COLUMN contact_snapshot VARCHAR(255) NULL;
ALTER TABLE service_orders ADD COLUMN cancel_reason VARCHAR(500) NULL;
ALTER TABLE service_orders ADD COLUMN service_fee_id BIGINT NULL;
ALTER TABLE service_orders ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE service_orders ADD CONSTRAINT fk_service_order_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id);

CREATE INDEX idx_shops_status_zone_rating ON shops (status, campus_zone, rating);
CREATE INDEX idx_service_items_shop_status ON service_items (shop_id, status);
CREATE INDEX idx_service_items_category_status ON service_items (category, status);
CREATE INDEX idx_service_orders_provider_status_time ON service_orders (provider_id, status, appointment_time);
CREATE INDEX idx_service_orders_customer_time ON service_orders (customer_id, appointment_time);

UPDATE service_items SET status = 'PUBLISHED' WHERE status = 'AVAILABLE';
UPDATE service_orders SET status = 'REQUESTED' WHERE status = 'CREATED';
