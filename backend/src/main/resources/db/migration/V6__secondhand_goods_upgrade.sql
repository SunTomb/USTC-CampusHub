ALTER TABLE goods
    ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER trade_location,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'INTENT_ONLY' AFTER campus_zone,
    ADD COLUMN delivery_method VARCHAR(40) NOT NULL DEFAULT 'OFFLINE_MEETUP' AFTER contact_visibility,
    ADD COLUMN service_fee_policy VARCHAR(40) NOT NULL DEFAULT 'NONE' AFTER delivery_method,
    ADD COLUMN published_at DATETIME NULL AFTER service_fee_policy,
    ADD COLUMN updated_at DATETIME NULL AFTER published_at,
    ADD COLUMN sold_at DATETIME NULL AFTER updated_at,
    ADD COLUMN sold_to_user_id BIGINT NULL AFTER sold_at,
    ADD CONSTRAINT fk_goods_sold_to_user FOREIGN KEY (sold_to_user_id) REFERENCES users(id);

CREATE TABLE goods_intents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    message VARCHAR(500) NULL,
    contact_snapshot VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    service_fee_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_goods_intent_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
    CONSTRAINT fk_goods_intent_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id),
    CONSTRAINT uk_goods_intent_buyer_goods UNIQUE (goods_id, buyer_id),
    INDEX idx_goods_intent_seller_status (seller_id, status),
    INDEX idx_goods_intent_buyer_time (buyer_id, created_at)
);

CREATE INDEX idx_goods_status_zone_time ON goods (status, campus_zone, created_at);
CREATE INDEX idx_reviews_target_user_time ON reviews (target_user_id, created_at);
