INSERT INTO roles (id, code, name, description) VALUES
(1, 'ROLE_STUDENT', '学生用户', '校园平台普通学生'),
(2, 'ROLE_ADMIN', '管理员', '内容审核与平台治理管理员');

INSERT INTO users (id, student_no, username, password_hash, real_name, nickname, phone, email, credit_score, status) VALUES
(1, 'PB23000001', 'alice', '$2a$10$wS5FoBrsKw1zPvSL7cm/0OjYbZ0fNn8b9hDwa2Y3L4Wt0t3Hg8e1a', '张晴', '晴天同学', '13800000001', 'alice@campus.example', 100, 'ACTIVE'),
(2, 'PB23000002', 'bob', '$2a$10$wS5FoBrsKw1zPvSL7cm/0OjYbZ0fNn8b9hDwa2Y3L4Wt0t3Hg8e1a', '李舟', '小舟', '13800000002', 'bob@campus.example', 96, 'ACTIVE'),
(3, 'PB23000003', 'carol', '$2a$10$wS5FoBrsKw1zPvSL7cm/0OjYbZ0fNn8b9hDwa2Y3L4Wt0t3Hg8e1a', '陈澄', '摄影Carol', '13800000003', 'carol@campus.example', 105, 'ACTIVE'),
(4, 'ADMIN0001', 'admin', '$2a$10$wS5FoBrsKw1zPvSL7cm/0OjYbZ0fNn8b9hDwa2Y3L4Wt0t3Hg8e1a', '管理员', 'CampusHub管理员', '13800000004', 'admin@campus.example', 100, 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(4, 2);

INSERT INTO wallet_accounts (id, user_id, balance, frozen_balance, status) VALUES
(1, 1, 200.00, 0.00, 'ACTIVE'),
(2, 2, 120.00, 0.00, 'ACTIVE'),
(3, 3, 300.00, 0.00, 'ACTIVE'),
(4, 4, 0.00, 0.00, 'ACTIVE');

INSERT INTO categories (id, parent_id, name, sort_order, enabled) VALUES
(1, NULL, '学习资料', 1, TRUE),
(2, NULL, '数码电子', 2, TRUE),
(3, NULL, '生活用品', 3, TRUE),
(4, NULL, '运动户外', 4, TRUE),
(5, 2, '电脑配件', 1, TRUE),
(6, 3, '宿舍用品', 1, TRUE);

INSERT INTO goods (id, seller_id, category_id, title, description, price, original_price, condition_level, trade_location, status, view_count) VALUES
(1, 1, 5, '九成新机械键盘', '青轴机械键盘，带原包装，适合宿舍学习和编程。', 129.00, 299.00, 'LIKE_NEW', '东区图书馆门口', 'ON_SALE', 36),
(2, 2, 1, '数据库系统概论教材', '课程同款教材，少量笔记，适合期末复习。', 35.00, 59.00, 'GOOD', '西区教学楼', 'ON_SALE', 52),
(3, 3, 6, '宿舍小台灯', '三档亮度，可 USB 供电。', 25.00, 49.00, 'GOOD', '中区宿舍楼下', 'PENDING_REVIEW', 8);

INSERT INTO goods_orders (id, order_no, goods_id, buyer_id, seller_id, amount, service_fee, status, contact_snapshot, paid_at) VALUES
(1, 'GO202605190001', 2, 1, 2, 35.00, 0.70, 'PAID', '晴天同学 13800000001', CURRENT_TIMESTAMP(6));

INSERT INTO reward_tasks (id, publisher_id, title, description, reward_amount, deposit_amount, task_location, deadline, status) VALUES
(1, 1, '帮取东区快递', '东区菜鸟驿站取两个小件，送到图书馆自习区。', 6.00, 6.00, '东区菜鸟驿站', DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), 'PUBLISHED'),
(2, 2, '晚饭代取', '帮忙在二食堂取打包饭，送到西区宿舍楼下。', 5.00, 5.00, '二食堂', DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 6 HOUR), 'PUBLISHED');

INSERT INTO task_applications (id, task_id, applicant_id, message, status) VALUES
(1, 1, 2, '我晚上正好路过东区，可以帮取。', 'APPLIED');

INSERT INTO shops (id, owner_id, name, description, service_area, status, rating) VALUES
(1, 3, 'Carol 摄影小铺', '校园写真、证件照精修、活动跟拍。', '东区/中区', 'APPROVED', 4.90),
(2, 2, '小舟数码维护', '电脑清灰、系统安装、手机贴膜。', '全校', 'APPROVED', 4.80);

INSERT INTO service_items (id, shop_id, title, description, price, duration_minutes, status) VALUES
(1, 1, '校园写真半小时', '校园内 30 分钟拍摄，含 6 张精修。', 59.00, 30, 'AVAILABLE'),
(2, 2, '笔记本电脑清灰', '基础清灰和散热检查。', 39.00, 45, 'AVAILABLE'),
(3, 2, '手机贴膜', '自带膜或店主提供普通高清膜。', 15.00, 10, 'AVAILABLE');

INSERT INTO service_orders (id, order_no, service_item_id, customer_id, provider_id, appointment_time, amount, service_fee, status, note, paid_at) VALUES
(1, 'SO202605190001', 1, 1, 3, DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), 59.00, 1.18, 'PAID', '想在图书馆附近拍摄', CURRENT_TIMESTAMP(6));

INSERT INTO project_ads (id, publisher_id, title, description, link_url, contact_info, status, view_count) VALUES
(1, 1, '数据库课程设计组队招募', '计划做校园交易与微服务平台，寻找前端和测试同学。', NULL, 'alice@campus.example', 'APPROVED', 88),
(2, 3, '摄影作品集展示', '展示校园活动摄影和毕业季样片，欢迎约拍合作。', NULL, 'carol@campus.example', 'APPROVED', 41);

INSERT INTO favorites (user_id, target_type, target_id) VALUES
(1, 'GOODS', 1),
(2, 'PROJECT_AD', 1),
(3, 'REWARD_TASK', 1);

INSERT INTO comments (user_id, target_type, target_id, content, status) VALUES
(2, 'GOODS', 1, '键帽有明显磨损吗？', 'VISIBLE'),
(1, 'PROJECT_AD', 1, '欢迎对 Vue 或 Spring Boot 感兴趣的同学联系。', 'VISIBLE');

INSERT INTO reviews (reviewer_id, target_user_id, target_type, target_id, rating, content) VALUES
(1, 2, 'GOODS_ORDER', 1, 5, '教材保存很好，交易沟通顺畅。');

INSERT INTO wallet_flows (wallet_account_id, user_id, flow_no, direction, amount, balance_after, business_type, business_id, remark) VALUES
(1, 1, 'WF202605190001', 'OUT', 35.70, 164.30, 'GOODS_ORDER', 1, '购买二手教材并支付服务费'),
(2, 2, 'WF202605190002', 'IN', 35.00, 155.00, 'GOODS_ORDER', 1, '二手教材收入');

INSERT INTO service_fee_records (fee_no, payer_id, target_type, target_id, amount, status, paid_at) VALUES
('SF202605190001', 1, 'GOODS_ORDER', 1, 0.70, 'PAID', CURRENT_TIMESTAMP(6)),
('SF202605190002', 1, 'SERVICE_ORDER', 1, 1.18, 'PAID', CURRENT_TIMESTAMP(6));

INSERT INTO file_resources (id, uploader_id, original_name, storage_path, content_type, size_bytes, status) VALUES
(1, 1, 'keyboard.jpg', './data/uploads/demo/keyboard.jpg', 'image/jpeg', 102400, 'AVAILABLE'),
(2, 3, 'photo-sample.jpg', './data/uploads/demo/photo-sample.jpg', 'image/jpeg', 204800, 'AVAILABLE');

INSERT INTO file_bindings (file_id, target_type, target_id, usage_type, sort_order) VALUES
(1, 'GOODS', 1, 'COVER', 1),
(2, 'PROJECT_AD', 2, 'IMAGE', 1);

INSERT INTO review_records (reviewer_id, target_type, target_id, result, reason) VALUES
(4, 'GOODS', 1, 'APPROVED', '信息完整，允许发布'),
(4, 'PROJECT_AD', 1, 'APPROVED', '项目招募内容合规');

INSERT INTO report_records (reporter_id, target_type, target_id, reason, description, status) VALUES
(2, 'COMMENT', 2, '内容待核验', '示例举报记录，用于管理员审核演示。', 'PENDING');

INSERT INTO violation_records (user_id, report_id, violation_type, description, credit_delta) VALUES
(2, NULL, 'LATE_DELIVERY', '示例轻微违约记录：任务响应超时。', -4);

INSERT INTO safety_logs (user_id, action, ip_address, user_agent, detail) VALUES
(1, 'LOGIN_SUCCESS', '127.0.0.1', 'CampusHub Demo', '本地演示登录成功'),
(4, 'REVIEW_APPROVE', '127.0.0.1', 'CampusHub Demo', '管理员审核通过示例内容');

INSERT INTO login_sessions (user_id, token_id, ip_address, user_agent, expires_at, revoked) VALUES
(1, 'demo-session-alice', '127.0.0.1', 'CampusHub Demo', DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 12 HOUR), FALSE);
