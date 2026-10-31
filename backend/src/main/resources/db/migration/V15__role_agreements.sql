CREATE TABLE role_agreement_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agreement_code VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    effective_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_role_agreement_version UNIQUE (agreement_code, version)
);
CREATE INDEX idx_role_agreement_active ON role_agreement_versions (agreement_code, status, effective_at);

CREATE TABLE role_agreement_consents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    agreement_version_id BIGINT NOT NULL,
    accepted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    source VARCHAR(30) NOT NULL DEFAULT 'ROLE_APPLICATION',
    CONSTRAINT uk_role_agreement_consent UNIQUE (user_id, role_type, agreement_version_id),
    CONSTRAINT fk_role_agreement_consent_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_agreement_consent_version FOREIGN KEY (agreement_version_id) REFERENCES role_agreement_versions(id)
);
CREATE INDEX idx_role_agreement_consent_user_time ON role_agreement_consents (user_id, accepted_at);

ALTER TABLE role_applications ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE role_applications ADD COLUMN deposit_policy VARCHAR(30) NOT NULL DEFAULT 'LEGACY_DEPOSIT';
ALTER TABLE role_applications ADD COLUMN agreement_version_id BIGINT NULL;
ALTER TABLE role_applications ADD COLUMN agreement_consent_id BIGINT NULL;
ALTER TABLE role_applications ADD COLUMN agreement_status VARCHAR(30) NOT NULL DEFAULT 'LEGACY';
ALTER TABLE role_applications ADD COLUMN agreement_consented_at DATETIME(6) NULL;
ALTER TABLE role_applications ADD COLUMN activation_mode VARCHAR(30) NOT NULL DEFAULT 'LEGACY_DEPOSIT';
ALTER TABLE role_applications ADD CONSTRAINT fk_role_application_agreement_version FOREIGN KEY (agreement_version_id) REFERENCES role_agreement_versions(id);
ALTER TABLE role_applications ADD CONSTRAINT fk_role_application_agreement_consent FOREIGN KEY (agreement_consent_id) REFERENCES role_agreement_consents(id);
CREATE INDEX idx_role_application_agreement ON role_applications (agreement_version_id, agreement_status);

INSERT INTO role_agreement_versions (agreement_code, version, title, content) VALUES
('USER_SERVICE', 'v1.0', '蜗壳 CampusHub 用户服务协议',
'一、服务性质与适用范围
蜗壳 CampusHub 由独立学生项目维护者提供校园信息发布、联系意向、服务预约、站内通知及问题反馈功能，不代表任何学校或机构的官方立场。校园邮箱验证用于确认邮箱可用性，不构成实名认证、学籍认证或履约担保。跑腿、闲置发布和店主理人身份的新增申请还需明确同意相应专项协议。

二、交易与资金边界【重要】
平台当前不开放充值、提现、平台收款、服务费支付、身份保证金或线上担保，不代收交易价款，不提供资金托管或自动赔付。物品价款、跑腿报酬及服务价格由参与者自行约定并直接结算。请当面核验、谨慎预付并保存交接凭证。既有资金与订单记录不因暂停而被清除；历史事项应由站务另行核对，暂停不等于原有权益或义务消灭。

三、账号与发布规范
用户应对自己发布的信息及账号使用行为负责，提供可联系的邮箱和联系方式；不得冒用他人身份、发布违法或侵权内容、欺诈、恶意刷单、骚扰或转售他人联系方式。不得在公开描述中展示取件码、密码、身份证号码等不必要信息。真实描述物品和服务，不以虚假承诺诱导付款。

四、个人信息处理【重要】
处理者为蜗壳 CampusHub 项目维护者。用于账号验证、履约联络和争议核查的身份联系信息限于用户在平台实际提供的邮箱及联系方式（如微信、QQ），不把系统占位学号、姓名或电话当作已验证身份。协议确认记录仅保存账号、对应身份、协议版本和服务器记录的同意时间，不额外采集设备指纹或 IP。
在收到与具体业务有关的举报、履约争议或安全事件后，经授权的站务可为核对当事人及协助联系而在必要范围内查阅上述信息及相关业务记录，不得用于无关营销、公开曝光或转交无关人员。向其他个人信息处理者提供信息或公开信息时，应遵守相应告知、单独同意等法律要求；依法履行法定义务或处理紧急安全事件的，依照适用法律办理。一次勾选本协议不构成任意对外披露授权。
必要信息仅在提供服务、处理未结争议及法律要求的期限内保存；不再需要时依法删除或匿名化。用户可通过页面反馈入口或已有站务联系渠道提出查阅、更正、删除、撤回同意等请求，平台核实后依法处理。撤回不影响撤回前基于同意开展的处理，也不当然免除既有订单义务。

五、争议处理与责任【重要】
平台可接收反馈、核查站内记录、联系双方，并在有合理依据时采取必要的内容下架或相关功能限制。平台不替代公安、法院或其他有权机构认定盗窃、诈骗及最终责任；涉嫌违法的，当事人可依法报案或寻求司法救济。用户可以提交证据和异议，详细处置与申诉规则将另行公示。本协议不排除或减轻平台依法应承担的义务和责任，不免除因故意、重大过失或人身损害依法不能免除的责任。

六、版本与生效
专项身份协议由用户阅读后主动勾选并提交，自成功提交时对该次身份申请生效；未经勾选不得视为已接受。已开通身份继续保留，不补写或伪造历史同意记录。后续实质性变更将以新版本公布，涉及个人信息处理目的、方式或种类变化时依法重新告知并取得必要同意。法律强制性规定优先适用。'),
('RUNNER_SERVICE', 'v1.0', '顺路跑腿服务协议',
'一、身份与服务
蜗壳 CampusHub 是独立学生项目，不代表任何学校或机构。新申请人阅读本协议、主动勾选并提交后可免费开通顺路跑腿员身份，不缴纳保证金。平台只提供任务与记录功能；校园邮箱验证不等于实名认证或履约担保。

二、接单与受托物品【重要】
接单前确认物品、路线、时限、报酬及交接要求，只承接自己具备能力且合法的事项。不得侵占、擅自食用、拆封、调换、转交、扣留或损坏受托餐品、物品和资料，不得将取件码和他人联系方式用于与本单无关的目的。不得以虚假送达记录、冒领或失联逃避交付。遇到无法取件、损坏、延误、突发安全问题，应及时联系委托人并通过任务问题反馈保留记录，不得继续作出无法履行的承诺。

三、费用与交接【重要】
报酬及实际必要费用由双方事先明确约定并直接结算；不得未经同意加价或以扣留物品强迫付款。平台目前不收取身份保证金，不提供充值、提现、代收或线上担保，不承诺自动赔付。优先在安全公共场所交接，保护取件凭证，遵守交通及校园管理规则。

四、必要信息核查【重要】
为处理具体任务的履约争议（包括疑似侵占餐品或物品）、举报和安全事件，经授权站务可在最小必要范围内查阅本人在平台实际提供的邮箱及联系方式，并核对相关任务和处理记录，用于核实当事人、联系沟通和依法配合调查。不得据此任意公开、营销或向无关人员提供资料。对外提供或公开个人信息须依法具备相应依据，履行告知并取得依法所需的单独同意；学校或个人的一般请求不自动构成无限披露授权。

五、争议与用户权利
参与者应如实提供相关证据，平台可协助联系、记录反馈并采取必要功能限制；最终责任由双方依法协商或有权机构认定。用户可通过反馈入口提出异议及个人信息查阅、更正、删除等请求。平台依法应承担的责任不因本协议免除。具体处罚和申诉细则后续另行公示，不以本协议自动罚款或扣取资金。

六、确认与记录
本人阅读并同意上述条款后提交申请。平台保存账号、角色、本协议版本、全文及服务器同意时间，不额外采集 IP 或设备指纹。数据在提供服务、处理未结争议及法定要求的必要期限内保存；不再需要时依法删除或匿名化。新申请要求当前版本；既有已开通身份不需要补签，不生成历史同意记录。'),
('GOODS_PUBLISHER_SERVICE', 'v1.0', '闲置发布与交易规范',
'一、身份与服务
蜗壳 CampusHub 是独立学生项目，不代表任何学校或机构。新申请人阅读本规范、主动勾选并提交后可免费开通闲置发布者身份，不缴纳保证金。平台提供信息发布、购买意向和问题反馈功能，邮箱验证不构成实名认证或商品质量担保。

二、发布真实性【重要】
仅发布本人有权处分的合法物品，如实说明成色、来源、缺陷、配件、价格和交接条件，不发布违禁品、侵权物品、虚假低价或误导性信息，不冒用他人图片和身份。明显缺陷和影响使用的情况应在交易前主动说明，不得伪造评价或交易记录。

三、交付与资金【重要】
达成约定后应按约交付；不能履行时及时联系对方协商，不得收款后失联、拒不交付、擅自替换物品或强加费用。当前平台不收取身份保证金，不提供充值、提现、平台收款或线上担保，价款由双方直接结算。建议安全公共场所当面验货，谨慎预付并保留沟通、付款和交接凭证。历史资金事项由站务另行核对，不因暂停而清除。

四、必要信息核查【重要】
为处理与具体商品有关的举报、未交付、描述不符或其他安全事件，经授权站务可在最小必要范围内查阅本人在平台实际提供的邮箱及联系方式，并核对相关业务记录，用于核实当事人、联系沟通和依法配合调查。不得据此任意公开资料、营销或转交无关人员。向其他个人信息处理者提供或公开信息时，须依法履行相应告知并取得依法所需的单独同意；本次勾选不等于授权公开曝光。

五、争议与责任
用户应如实提供证据，平台可协助沟通、保存处理记录并采取必要的内容下架或发布权限限制，不代替有权机构作最终责任认定，不承诺平台赔偿交易损失。平台依法应承担的责任不因本规范免除。用户可通过页面反馈提出异议及个人信息权利请求；详细处罚和申诉规则后续公示，不实施自动罚款。

六、确认与记录
平台保存账号、角色、协议版本、全文和服务器同意时间，不额外采集 IP 或设备指纹。资料仅在服务、未结争议处理及法定要求的必要期限内保存；不再需要时依法删除或匿名化。新申请须确认当前版本，已开通身份不强制补签，不伪造历史同意记录。'),
('SHOP_MERCHANT_SERVICE', 'v1.0', '技能店主理人服务协议',
'一、申请与核验
蜗壳 CampusHub 是独立学生项目，不代表任何学校或机构。申请人阅读本协议、主动勾选并提交后进入站务核验；核验通过方可取得店主理人身份，不缴纳保证金。核验不代表学校认证、专业资质认证或服务质量担保。

二、服务说明与履约【重要】
如实说明本人技能、服务范围、价格、耗时、限制条件和可能风险；需要法定资质的服务不得在无资质时承接。不发布违法、侵权、虚假或误导内容。接受预约前确认时间、地点、交付物和费用，不能履行时及时协商；不得收款后失联、拒不服务、扣留顾客物品或收取未约定费用。维修、拍摄等过程中应妥善保护物品及隐私，不擅自查看、复制或传播顾客设备数据、照片和联系方式。

三、交易与资金【重要】
平台当前不收取保证金，不提供充值、提现、代收、自动退款或线上担保；费用由双方直接约定和结算。双方应保留预约及交付记录，并选择安全适宜的服务地点。历史记录保留，未结事项由站务另行核对。

四、必要信息核查【重要】
为核验本次申请或处理具体预约纠纷、举报和安全事件，经授权站务可在必要范围内查阅本人在平台实际提供的邮箱和联系方式及相关业务记录，用于核对当事人、联系沟通和依法配合调查。不得任意公开、营销或提供给无关人员。对外提供或公开个人信息须具备合法依据并履行相应告知、单独同意等要求，不因一次勾选取得无限授权。

五、处置与责任
平台可根据相关证据要求说明、暂停展示或限制相关功能，并记录处理情况；平台不替代司法或其他有权机构作最终责任认定，不承诺赔偿服务损失。平台依法应承担的责任不因本协议免除。用户可通过反馈入口提出异议及个人信息权利请求，具体处罚、申诉细则后续公示，不实施自动罚款或扣款。

六、确认与记录
平台保存账号、角色、协议版本、全文及服务器同意时间，不额外采集 IP 或设备指纹。信息仅在核验、提供服务、处理未结争议和法定要求的必要期限内保存；不再需要时依法删除或匿名化。同意协议仅代表提交核验，不代表已获准开店。既有身份保留且无需补签，不补写历史同意记录。');
