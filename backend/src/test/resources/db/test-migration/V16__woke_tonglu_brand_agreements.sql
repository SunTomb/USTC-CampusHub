INSERT INTO role_agreement_versions (agreement_code, version, title, content, effective_at, status)
SELECT source_agreement.agreement_code,
       'v1.1',
       REPLACE(source_agreement.title, '蜗壳 CampusHub', '蜗壳同路 CampusHub'),
       REPLACE(source_agreement.content, '蜗壳 CampusHub', '蜗壳同路 CampusHub'),
       CURRENT_TIMESTAMP,
       'ACTIVE'
FROM role_agreement_versions source_agreement
LEFT JOIN role_agreement_versions existing_agreement
  ON existing_agreement.agreement_code = source_agreement.agreement_code
 AND existing_agreement.version = 'v1.1'
WHERE source_agreement.version = 'v1.0'
  AND source_agreement.status = 'ACTIVE'
  AND existing_agreement.id IS NULL
  AND source_agreement.agreement_code IN (
      'USER_SERVICE', 'RUNNER_SERVICE', 'GOODS_PUBLISHER_SERVICE', 'SHOP_MERCHANT_SERVICE'
  );

UPDATE role_agreement_versions
SET status = 'SUPERSEDED'
WHERE version = 'v1.0'
  AND status = 'ACTIVE'
  AND agreement_code IN (
      'USER_SERVICE', 'RUNNER_SERVICE', 'GOODS_PUBLISHER_SERVICE', 'SHOP_MERCHANT_SERVICE'
  );
