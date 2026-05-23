ALTER TABLE service_fee_records
    ADD COLUMN payment_order_no VARCHAR(64) NULL AFTER status,
    ADD COLUMN payment_provider VARCHAR(40) NULL AFTER payment_order_no,
    ADD COLUMN payment_center_order_no VARCHAR(80) NULL AFTER payment_provider,
    ADD COLUMN pay_url VARCHAR(1000) NULL AFTER payment_center_order_no,
    ADD COLUMN expires_at DATETIME NULL AFTER paid_at,
    ADD COLUMN failed_at DATETIME NULL AFTER expires_at,
    ADD COLUMN failure_reason VARCHAR(500) NULL AFTER failed_at,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER failure_reason,
    ADD UNIQUE KEY uk_service_fee_payment_order_no (payment_order_no),
    ADD INDEX idx_service_fee_payment_status_time (status, created_at);

CREATE TABLE payment_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    business_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_order_no VARCHAR(80) NULL,
    pay_url VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    expires_at DATETIME NULL,
    paid_at DATETIME NULL,
    failed_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_order_no UNIQUE (order_no),
    CONSTRAINT fk_payment_order_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    INDEX idx_payment_order_business (business_type, business_id),
    INDEX idx_payment_order_provider_order (provider_order_no),
    INDEX idx_payment_order_status_time (status, created_at)
);

CREATE TABLE payment_callback_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    provider_order_no VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    verified BOOLEAN NOT NULL,
    handled BOOLEAN NOT NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_callback_event_id UNIQUE (event_id),
    INDEX idx_payment_callback_order_time (order_no, created_at)
);

ALTER TABLE role_applications
    MODIFY COLUMN deposit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN deposit_payment_order_no VARCHAR(64) NULL AFTER deposit_status,
    ADD UNIQUE KEY uk_role_application_deposit_order (deposit_payment_order_no);
