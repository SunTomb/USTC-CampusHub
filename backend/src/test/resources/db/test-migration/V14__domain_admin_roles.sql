INSERT INTO roles (code, name, description)
SELECT 'ROLE_MASTER_ADMIN', '最高级系统管理员', '拥有平台全部后台权限'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_MASTER_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_TRADE_ADMIN', '交易管理员', '管理跑腿、二手与交易纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_TRADE_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_SHOWCASE_ADMIN', '展示管理员', '管理项目、店铺与服务纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_SHOWCASE_ADMIN');
