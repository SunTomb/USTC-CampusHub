ALTER TABLE wallet_accounts
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE wallet_flows
    ADD COLUMN flow_type VARCHAR(40) NOT NULL DEFAULT 'LEGACY' AFTER direction,
    ADD COLUMN available_balance_after DECIMAL(10,2) NULL AFTER balance_after,
    ADD COLUMN frozen_balance_after DECIMAL(10,2) NULL AFTER available_balance_after,
    ADD COLUMN idempotency_key VARCHAR(100) NULL AFTER business_id,
    ADD COLUMN counterparty_user_id BIGINT NULL AFTER idempotency_key,
    ADD COLUMN created_by VARCHAR(40) NOT NULL DEFAULT 'SYSTEM' AFTER counterparty_user_id,
    ADD COLUMN operator_id BIGINT NULL AFTER created_by,
    ADD CONSTRAINT uk_wallet_flows_idempotency_key UNIQUE (idempotency_key),
    ADD CONSTRAINT fk_wallet_flows_counterparty_user FOREIGN KEY (counterparty_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_wallet_flows_operator FOREIGN KEY (operator_id) REFERENCES users(id),
    ADD INDEX idx_wallet_flows_type_time (flow_type, created_at),
    ADD INDEX idx_wallet_flows_counterparty_time (counterparty_user_id, created_at);

UPDATE wallet_flows
SET available_balance_after = balance_after,
    frozen_balance_after = 0.00,
    flow_type = 'LEGACY',
    created_by = 'SYSTEM'
WHERE flow_type = 'LEGACY';

ALTER TABLE wallet_flows
    MODIFY COLUMN available_balance_after DECIMAL(10,2) NOT NULL,
    MODIFY COLUMN frozen_balance_after DECIMAL(10,2) NOT NULL;

CREATE TABLE wallet_recharge_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    channel_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(10,2) NOT NULL,
    payment_order_no VARCHAR(64) NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    review_note VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    paid_at DATETIME NULL,
    failed_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_recharge_order_no UNIQUE (order_no),
    CONSTRAINT fk_wallet_recharge_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wallet_recharge_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    INDEX idx_wallet_recharge_user_time (user_id, created_at),
    INDEX idx_wallet_recharge_status_time (status, created_at),
    INDEX idx_wallet_recharge_payment_order (payment_order_no)
);

CREATE TABLE wallet_withdrawal_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    account_snapshot VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
    review_note VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    completed_at DATETIME NULL,
    rejected_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_withdrawal_request_no UNIQUE (request_no),
    CONSTRAINT fk_wallet_withdrawal_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wallet_withdrawal_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    INDEX idx_wallet_withdrawal_user_time (user_id, created_at),
    INDEX idx_wallet_withdrawal_status_time (status, created_at)
);

CREATE TABLE wallet_frozen_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    freeze_no VARCHAR(64) NOT NULL,
    wallet_account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'FROZEN',
    reason VARCHAR(500) NULL,
    frozen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at DATETIME NULL,
    canceled_at DATETIME NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_frozen_freeze_no UNIQUE (freeze_no),
    CONSTRAINT fk_wallet_frozen_account FOREIGN KEY (wallet_account_id) REFERENCES wallet_accounts(id),
    CONSTRAINT fk_wallet_frozen_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_wallet_frozen_user_status (user_id, status, created_at),
    INDEX idx_wallet_frozen_target (target_type, target_id),
    INDEX idx_wallet_frozen_status_time (status, created_at)
);

ALTER TABLE goods_orders
    ADD COLUMN trade_mode VARCHAR(40) NOT NULL DEFAULT 'OFFLINE' AFTER service_fee,
    ADD COLUMN escrow_status VARCHAR(40) NOT NULL DEFAULT 'NONE' AFTER trade_mode,
    ADD COLUMN escrow_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER escrow_status,
    ADD COLUMN platform_service_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER escrow_amount,
    ADD COLUMN escrow_frozen_at DATETIME NULL AFTER canceled_at,
    ADD COLUMN escrow_released_at DATETIME NULL AFTER escrow_frozen_at,
    ADD COLUMN escrow_canceled_at DATETIME NULL AFTER escrow_released_at,
    ADD COLUMN escrow_disputed_at DATETIME NULL AFTER escrow_canceled_at,
    ADD COLUMN escrow_cancel_reason VARCHAR(500) NULL AFTER escrow_disputed_at,
    ADD COLUMN escrow_dispute_reason VARCHAR(500) NULL AFTER escrow_cancel_reason,
    ADD INDEX idx_goods_orders_trade_time (trade_mode, created_at),
    ADD INDEX idx_goods_orders_escrow_status_time (escrow_status, created_at),
    ADD INDEX idx_goods_orders_buyer_escrow (buyer_id, escrow_status, created_at),
    ADD INDEX idx_goods_orders_seller_escrow (seller_id, escrow_status, created_at);
