ALTER TABLE users
    ADD COLUMN wechat_contact VARCHAR(120) NULL AFTER email,
    ADD COLUMN qq_contact VARCHAR(60) NULL AFTER wechat_contact;
