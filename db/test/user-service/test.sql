USE user_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM user_favorite;
DELETE FROM user_profile_ext;
DELETE FROM operation_audit_log;
DELETE FROM user_address;
DELETE FROM role_permissions;
DELETE FROM user_roles;
DELETE FROM permissions;
DELETE FROM roles;
DELETE FROM merchant_auth;
DELETE FROM merchant;
DELETE FROM admin;
DELETE FROM users;
DELETE FROM test_access_token WHERE id = 1;

-- index optimization
ALTER TABLE users
    ADD INDEX idx_users_deleted_created (deleted, created_at);

ALTER TABLE users
    ADD INDEX idx_users_status_deleted_created (status, deleted, created_at);

ALTER TABLE merchant
    ADD INDEX idx_merchant_name_deleted (merchant_name, deleted);

INSERT INTO users (id, username, password, phone, nickname, avatar_url, email, status, enabled, deleted, version)
VALUES (20001, 't_user_20001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900000001',
        'Test User One', NULL, 't_user_20001@example.com', 1, 1, 0, 0),
       (20002, 't_user_20002', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900000002',
        'Test User Two', 'https://example.com/u2.png', 't_user_20002@example.com', 1, 1, 0, 0),
       (20003, 't_user_20003', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900000003',
        'Test User Three', NULL, 't_user_20003@example.com', 1, 1, 0, 0),
       (24001, 't_admin_24001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900010001',
        'Platform Admin', NULL, 't_admin_24001@example.com', 1, 1, 0, 0),
       (30001, 't_merchant_30001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900020001',
        'Merchant Owner A', NULL, 't_merchant_30001@example.com', 1, 1, 0, 0),
       (30002, 't_merchant_30002', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900020002',
        'Merchant Owner B', 'https://example.com/u30002.png', 't_merchant_30002@example.com', 1, 1, 0, 0);

INSERT INTO roles (id, code, name, description, deleted, version)
VALUES (26001, 'ROLE_USER', 'User', 'Default customer role', 0, 0),
       (26002, 'ROLE_MERCHANT', 'Merchant', 'Merchant operation role', 0, 0),
       (26003, 'ROLE_ADMIN', 'Admin', 'Platform administration role', 0, 0);

INSERT INTO permissions (id, code, name, module, deleted, version)
VALUES (27001, 'order:create', 'Create order', 'order', 0, 0),
       (27002, 'order:cancel', 'Cancel order', 'order', 0, 0),
       (27003, 'order:query', 'Query order', 'order', 0, 0),
       (27004, 'order:refund', 'Request refund', 'order', 0, 0),
       (27011, 'product:view', 'View product', 'product', 0, 0),
       (27012, 'product:create', 'Create product', 'product', 0, 0),
       (27013, 'product:edit', 'Edit product', 'product', 0, 0),
       (27014, 'product:delete', 'Delete product', 'product', 0, 0),
       (27021, 'user:profile', 'Edit profile', 'user', 0, 0),
       (27022, 'user:address', 'Manage address', 'user', 0, 0),
       (27031, 'merchant:manage', 'Manage merchant', 'merchant', 0, 0),
       (27032, 'merchant:audit', 'Audit merchant', 'merchant', 0, 0),
       (27041, 'admin:all', 'Full admin access', 'admin', 0, 0);

INSERT INTO role_permissions (id, role_id, permission_id, deleted, version)
VALUES (28001, 26001, 27001, 0, 0),
       (28002, 26001, 27002, 0, 0),
       (28003, 26001, 27003, 0, 0),
       (28004, 26001, 27004, 0, 0),
       (28005, 26001, 27011, 0, 0),
       (28006, 26001, 27021, 0, 0),
       (28007, 26001, 27022, 0, 0),
       (28011, 26002, 27011, 0, 0),
       (28012, 26002, 27012, 0, 0),
       (28013, 26002, 27013, 0, 0),
       (28014, 26002, 27014, 0, 0),
       (28015, 26002, 27031, 0, 0),
       (28016, 26002, 27003, 0, 0),
       (28031, 26003, 27041, 0, 0),
       (28032, 26003, 27032, 0, 0),
       (28033, 26003, 27031, 0, 0),
       (28034, 26003, 27011, 0, 0),
       (28035, 26003, 27012, 0, 0),
       (28036, 26003, 27013, 0, 0),
       (28037, 26003, 27014, 0, 0),
       (28038, 26003, 27001, 0, 0),
       (28039, 26003, 27002, 0, 0),
       (28040, 26003, 27003, 0, 0),
       (28041, 26003, 27004, 0, 0),
       (28042, 26003, 27021, 0, 0),
       (28043, 26003, 27022, 0, 0);

INSERT INTO user_roles (id, user_id, role_id, deleted, version)
VALUES (29001, 20001, 26001, 0, 0),
       (29002, 20002, 26001, 0, 0),
       (29003, 20003, 26001, 0, 0),
       (29004, 24001, 26003, 0, 0),
       (29005, 30001, 26001, 0, 0),
       (29006, 30001, 26002, 0, 0),
       (29007, 30002, 26001, 0, 0),
       (29008, 30002, 26002, 0, 0);

INSERT INTO user_profile_ext (id, user_id, gender, birthday, bio, country, province, city, personal_tags, preferences, deleted, version)
VALUES (21001, 20001, 'MALE', '1995-01-01', 'Enjoys smart devices and coffee.', 'China', 'Shanghai', 'Shanghai',
        JSON_ARRAY('vip', 'tech'), JSON_OBJECT('lang', 'zh-CN', 'theme', 'light'), 0, 0),
       (21002, 20002, 'FEMALE', '1997-03-15', 'Prefers wearables and lifestyle products.', 'China', 'Hangzhou', 'Hangzhou',
        JSON_ARRAY('new-user', 'fitness'), JSON_OBJECT('lang', 'zh-CN', 'theme', 'light'), 0, 0),
       (21003, 20003, 'MALE', '1992-11-08', 'Focuses on foldables and productivity tools.', 'China', 'Jiangsu', 'Nanjing',
        JSON_ARRAY('business', 'early-adopter'), JSON_OBJECT('lang', 'zh-CN', 'theme', 'dark'), 0, 0);

INSERT INTO user_address (id, user_id, address_tag, receiver_name, receiver_phone, country, province, city, district, street,
                          detail_address, postal_code, longitude, latitude, is_default, deleted, version)
VALUES (22001, 20001, 'HOME', 'Test User One', '13900000001', 'China', 'Shanghai', 'Shanghai', 'Pudong', 'Century Ave',
        'No.200', '200120', 121.4737000, 31.2304000, 1, 0, 0),
       (22002, 20001, 'OFFICE', 'Test User One', '13900000001', 'China', 'Shanghai', 'Shanghai', 'Xuhui', 'Hongqiao Rd',
        'Room 1808', '200030', 121.4375000, 31.1880000, 0, 0, 0),
       (22003, 20002, 'HOME', 'Test User Two', '13900000002', 'China', 'Zhejiang', 'Hangzhou', 'Yuhang', 'Wenyi West Rd',
        'Building 3', '311100', 120.0242000, 30.2799000, 1, 0, 0),
       (22004, 20003, 'HOME', 'Test User Three', '13900000003', 'China', 'Jiangsu', 'Nanjing', 'Jianye', 'Jiangdong Middle Rd',
        'Cloud Center Tower A', '210019', 118.7364000, 32.0049000, 1, 0, 0);

INSERT INTO user_favorite (id, user_id, spu_id, sku_id, favorite_status, deleted, version)
VALUES (23001, 20001, 50001, 51001, 'ACTIVE', 0, 0),
       (23002, 20001, 50004, 51006, 'ACTIVE', 0, 0),
       (23003, 20002, 50006, 51008, 'ACTIVE', 0, 0),
       (23004, 20003, 50003, 51004, 'ACTIVE', 0, 0),
       (23005, 20003, 50005, 51007, 'ACTIVE', 0, 0);

INSERT INTO admin (id, username, real_name, phone, role, status, deleted, version)
VALUES (24001, 't_admin_24001', 'Platform Admin', '13900010001', 'ADMIN', 1, 0, 0);

INSERT INTO merchant (id, username, merchant_name, phone, status, audit_status, deleted, version)
VALUES (30001, 't_merchant_30001', 'Cloud Devices Flagship', '13900020001', 1, 1, 0, 0),
       (30002, 't_merchant_30002', 'Cloud Life Store', '13900020002', 1, 1, 0, 0);

INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status, auth_remark, deleted, version)
VALUES (25001, 30001, 'BL-TEST-30001', 'https://example.com/license-30001.png',
        'https://example.com/idf-30001.png', 'https://example.com/idb-30001.png',
        '13900020001', 'Shanghai Test Street 1', 1, 'approved', 0, 0),
       (25002, 30002, 'BL-TEST-30002', 'https://example.com/license-30002.png',
        'https://example.com/idf-30002.png', 'https://example.com/idb-30002.png',
        '13900020002', 'Hangzhou Test Street 8', 1, 'approved', 0, 0);

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1);
