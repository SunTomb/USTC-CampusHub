ALTER TABLE shops
    ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER service_area,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'ORDER_ONLY' AFTER campus_zone,
    ADD COLUMN opening_hours VARCHAR(255) NULL AFTER contact_visibility,
    ADD COLUMN cover_file_id BIGINT NULL AFTER opening_hours,
    ADD CONSTRAINT fk_shop_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_items
    ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER shop_id,
    ADD COLUMN min_price DECIMAL(10,2) NULL AFTER description,
    ADD COLUMN max_price DECIMAL(10,2) NULL AFTER min_price,
    ADD COLUMN price_unit VARCHAR(30) NOT NULL DEFAULT '次' AFTER price,
    ADD COLUMN cover_file_id BIGINT NULL AFTER price_unit,
    ADD CONSTRAINT fk_service_item_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_orders
    ADD COLUMN contact_snapshot VARCHAR(255) NULL AFTER note,
    ADD COLUMN cancel_reason VARCHAR(500) NULL AFTER contact_snapshot,
    ADD COLUMN service_fee_id BIGINT NULL AFTER cancel_reason,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT fk_service_order_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id);

CREATE INDEX idx_shops_status_zone_rating ON shops (status, campus_zone, rating);
CREATE INDEX idx_service_items_shop_status ON service_items (shop_id, status);
CREATE INDEX idx_service_items_category_status ON service_items (category, status);
CREATE INDEX idx_service_orders_provider_status_time ON service_orders (provider_id, status, appointment_time);
CREATE INDEX idx_service_orders_customer_time ON service_orders (customer_id, appointment_time);

UPDATE service_items SET status = 'PUBLISHED' WHERE status = 'AVAILABLE';
UPDATE service_orders SET status = 'REQUESTED' WHERE status = 'CREATED';
