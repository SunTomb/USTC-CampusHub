INSERT INTO roles (code, name, description)
SELECT 'ROLE_MASTER_ADMIN', '最高级系统管理员', '拥有 CampusHub 全部后台权限'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_MASTER_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_TRADE_ADMIN', '交易管理员', '管理悬赏跑腿、二手商品与交易售后纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_TRADE_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_SHOWCASE_ADMIN', '展示管理员', '管理项目广告、学生店铺与服务售后纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_SHOWCASE_ADMIN');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ROLE_MASTER_ADMIN'
WHERE (u.email = 'yeshenghao@mail.ustc.edu.cn' OR u.username = 'yeshenghao')
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ROLE_ADMIN'
WHERE (u.email = 'yeshenghao@mail.ustc.edu.cn' OR u.username = 'yeshenghao')
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
