CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) NOT NULL,
    avatar_url VARCHAR(512),
    credit_score INT NOT NULL DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_student_no UNIQUE (student_no),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2),
    condition_level VARCHAR(32) NOT NULL,
    trade_location VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_goods_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_goods_category FOREIGN KEY (category_id) REFERENCES categories (id),
    INDEX idx_goods_status_created (status, created_at),
    INDEX idx_goods_seller (seller_id),
    INDEX idx_goods_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE goods_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    goods_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    service_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    contact_snapshot VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    paid_at DATETIME(6),
    completed_at DATETIME(6),
    canceled_at DATETIME(6),
    CONSTRAINT uk_goods_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_goods_orders_goods FOREIGN KEY (goods_id) REFERENCES goods (id),
    CONSTRAINT fk_goods_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT fk_goods_orders_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    INDEX idx_goods_orders_buyer (buyer_id, created_at),
    INDEX idx_goods_orders_seller (seller_id, created_at),
    INDEX idx_goods_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reward_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    publisher_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    reward_amount DECIMAL(10, 2) NOT NULL,
    deposit_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    task_location VARCHAR(128) NOT NULL,
    deadline DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reward_tasks_publisher FOREIGN KEY (publisher_id) REFERENCES users (id),
    INDEX idx_reward_tasks_status_deadline (status, deadline),
    INDEX idx_reward_tasks_publisher (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE task_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    message VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    accepted_at DATETIME(6),
    completed_at DATETIME(6),
    CONSTRAINT uk_task_applicant UNIQUE (task_id, applicant_id),
    CONSTRAINT fk_task_applications_task FOREIGN KEY (task_id) REFERENCES reward_tasks (id),
    CONSTRAINT fk_task_applications_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    INDEX idx_task_applications_applicant (applicant_id, created_at),
    INDEX idx_task_applications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shops (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    service_area VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    rating DECIMAL(3, 2) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_shops_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    INDEX idx_shops_owner (owner_id),
    INDEX idx_shops_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_service_items_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    INDEX idx_service_items_shop (shop_id),
    INDEX idx_service_items_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    service_item_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    appointment_time DATETIME(6) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    service_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    note VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    paid_at DATETIME(6),
    completed_at DATETIME(6),
    canceled_at DATETIME(6),
    CONSTRAINT uk_service_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_service_orders_item FOREIGN KEY (service_item_id) REFERENCES service_items (id),
    CONSTRAINT fk_service_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_service_orders_provider FOREIGN KEY (provider_id) REFERENCES users (id),
    INDEX idx_service_orders_customer (customer_id, created_at),
    INDEX idx_service_orders_provider (provider_id, created_at),
    INDEX idx_service_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_ads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    publisher_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    link_url VARCHAR(512),
    contact_info VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_project_ads_publisher FOREIGN KEY (publisher_id) REFERENCES users (id),
    INDEX idx_project_ads_status_created (status, created_at),
    INDEX idx_project_ads_publisher (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_favorites_target UNIQUE (user_id, target_type, target_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_favorites_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    parent_id BIGINT,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id),
    INDEX idx_comments_target (target_type, target_id, created_at),
    INDEX idx_comments_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reviewer_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_reviews_target UNIQUE (reviewer_id, target_type, target_id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_target_user FOREIGN KEY (target_user_id) REFERENCES users (id),
    INDEX idx_reviews_target_user (target_user_id, created_at),
    INDEX idx_reviews_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0,
    frozen_balance DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_wallet_accounts_user UNIQUE (user_id),
    CONSTRAINT fk_wallet_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_flows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    flow_no VARCHAR(64) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_wallet_flows_flow_no UNIQUE (flow_no),
    CONSTRAINT fk_wallet_flows_account FOREIGN KEY (wallet_account_id) REFERENCES wallet_accounts (id),
    CONSTRAINT fk_wallet_flows_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_wallet_flows_user (user_id, created_at),
    INDEX idx_wallet_flows_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_fee_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fee_no VARCHAR(64) NOT NULL,
    payer_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    paid_at DATETIME(6),
    CONSTRAINT uk_service_fee_records_fee_no UNIQUE (fee_no),
    CONSTRAINT fk_service_fee_records_payer FOREIGN KEY (payer_id) REFERENCES users (id),
    INDEX idx_service_fee_records_target (target_type, target_id),
    INDEX idx_service_fee_records_payer (payer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE file_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_file_resources_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    INDEX idx_file_resources_uploader (uploader_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE file_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    usage_type VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_file_bindings_target UNIQUE (file_id, target_type, target_id, usage_type),
    CONSTRAINT fk_file_bindings_file FOREIGN KEY (file_id) REFERENCES file_resources (id),
    INDEX idx_file_bindings_target (target_type, target_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE review_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reviewer_id BIGINT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    result VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_review_records_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    INDEX idx_review_records_target (target_type, target_id, created_at),
    INDEX idx_review_records_reviewer (reviewer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE report_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    handler_id BIGINT,
    handled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_report_records_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_report_records_handler FOREIGN KEY (handler_id) REFERENCES users (id),
    INDEX idx_report_records_target (target_type, target_id),
    INDEX idx_report_records_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE violation_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    report_id BIGINT,
    violation_type VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    credit_delta INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_violation_records_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_violation_records_report FOREIGN KEY (report_id) REFERENCES report_records (id),
    INDEX idx_violation_records_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE safety_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(128) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    detail VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_safety_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_safety_logs_user (user_id, created_at),
    INDEX idx_safety_logs_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE login_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_id VARCHAR(128) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_login_sessions_token_id UNIQUE (token_id),
    CONSTRAINT fk_login_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_login_sessions_user (user_id, created_at),
    INDEX idx_login_sessions_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
