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
        'Test User 1', 'https://example.com/u1.png', 't_user_20001@example.com', 1, 1, 0, 0),
       (24001, 't_admin_24001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900010001',
        'Test Admin 1', NULL, NULL, 1, 1, 0, 0),
       (30001, 't_merchant_30001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13900020001',
        'Merchant Owner', 'https://example.com/u2.png', 't_merchant_30001@example.com', 1, 1, 0, 0);

INSERT INTO roles (id, code, name, description, deleted, version)
VALUES (26001, 'ROLE_USER', '普通用户', '基础用户角色', 0, 0),
       (26002, 'ROLE_MERCHANT', '商家', '商家角色', 0, 0),
       (26003, 'ROLE_ADMIN', '管理员', '管理员角色', 0, 0);

INSERT INTO permissions (id, code, name, module, deleted, version)
VALUES (27001, 'order:create', '创建订单', 'order', 0, 0),
       (27002, 'order:cancel', '取消订单', 'order', 0, 0),
       (27003, 'order:query', '查询订单', 'order', 0, 0),
       (27004, 'order:refund', '申请退款', 'order', 0, 0),
       (27011, 'product:view', '查看商品', 'product', 0, 0),
       (27012, 'product:create', '创建商品', 'product', 0, 0),
       (27013, 'product:edit', '编辑商品', 'product', 0, 0),
       (27014, 'product:delete', '删除商品', 'product', 0, 0),
       (27021, 'user:profile', '编辑资料', 'user', 0, 0),
       (27022, 'user:address', '管理地址', 'user', 0, 0),
       (27031, 'merchant:manage', '商家管理', 'merchant', 0, 0),
       (27032, 'merchant:audit', '商家审核', 'merchant', 0, 0),
       (27041, 'admin:all', '所有权限', 'admin', 0, 0);

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
       (28043, 26003, 27022, 0, 0),
       (28051, 26011, 27041, 0, 0),
       (28052, 26011, 27032, 0, 0),
       (28053, 26011, 27031, 0, 0),
       (28054, 26011, 27011, 0, 0),
       (28055, 26011, 27012, 0, 0),
       (28056, 26011, 27013, 0, 0),
       (28057, 26011, 27014, 0, 0),
       (28058, 26011, 27001, 0, 0),
       (28059, 26011, 27002, 0, 0),
       (28060, 26011, 27003, 0, 0),
       (28061, 26011, 27004, 0, 0),
       (28062, 26011, 27021, 0, 0),
       (28063, 26011, 27022, 0, 0),
       (28071, 26012, 27041, 0, 0),
       (28072, 26012, 27032, 0, 0),
       (28073, 26012, 27031, 0, 0),
       (28074, 26012, 27011, 0, 0),
       (28075, 26012, 27012, 0, 0),
       (28076, 26012, 27013, 0, 0),
       (28077, 26012, 27014, 0, 0),
       (28078, 26012, 27001, 0, 0),
       (28079, 26012, 27002, 0, 0),
       (28080, 26012, 27003, 0, 0),
       (28081, 26012, 27004, 0, 0),
       (28082, 26012, 27021, 0, 0),
       (28083, 26012, 27022, 0, 0);

INSERT INTO user_roles (id, user_id, role_id, deleted, version)
VALUES (29001, 20001, 26001, 0, 0),
       (29002, 24001, 26003, 0, 0),
       (29003, 24001, 26011, 0, 0),
       (29004, 30001, 26001, 0, 0),
       (29005, 30001, 26002, 0, 0);

INSERT INTO user_profile_ext (id, user_id, gender, birthday, bio, country, province, city, personal_tags, preferences, deleted, version)
VALUES (21001, 20001, 'MALE', '1995-01-01', 'test profile', 'China', 'Shanghai', 'Shanghai',
        JSON_ARRAY('vip', 'tech'), JSON_OBJECT('lang', 'zh-CN'), 0, 0);

INSERT INTO user_address (id, user_id, address_tag, receiver_name, receiver_phone, country, province, city, district, street,
                          detail_address, postal_code, longitude, latitude, is_default, deleted, version)
VALUES (22001, 20001, 'HOME', 'Test User 1', '13900000001', 'China', 'Shanghai', 'Shanghai', 'Pudong', 'Century Ave',
        'No.200', '200120', 121.4737000, 31.2304000, 1, 0, 0);

INSERT INTO user_favorite (id, user_id, spu_id, sku_id, favorite_status, deleted, version)
VALUES (23001, 20001, 40001, 41001, 'ACTIVE', 0, 0);

INSERT INTO admin (id, username, real_name, phone, role, status, deleted, version)
VALUES (24001, 't_admin_24001', 'Test Admin 1', '13900010001', 'ADMIN', 1, 0, 0);

INSERT INTO merchant (id, username, merchant_name, phone, status, audit_status, deleted, version)
VALUES (30001, 't_merchant_30001', 'Test Merchant A', '13900020001', 1, 1, 0, 0);

INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status, auth_remark, deleted, version)
VALUES (25001, 30001, 'BL-TEST-30001', 'https://example.com/license-30001.png',
        'https://example.com/idf-30001.png', 'https://example.com/idb-30001.png',
        '13900020001', 'Shanghai Test Street 1', 1, 'approved', 0, 0);

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1);
