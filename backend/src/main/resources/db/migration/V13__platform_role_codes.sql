INSERT INTO roles (code, name, description)
SELECT 'ROLE_RUNNER', '跑腿接单员', '可接取校园跑腿任务'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_RUNNER');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_GOODS_PUBLISHER', '二手发布者', '可发布二手商品'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_GOODS_PUBLISHER');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_SHOP_MERCHANT', '学生店主', '可经营学生技能店铺'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_SHOP_MERCHANT');
