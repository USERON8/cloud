USE user_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM user_favorite;
DELETE FROM user_profile_ext;
DELETE FROM sys_role_menu;
DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_permission;
DELETE FROM sys_role;
DELETE FROM sys_menu;
DELETE FROM operation_audit_log;
DELETE FROM user_address;
DELETE FROM merchant_auth;
DELETE FROM merchant;
DELETE FROM admin;
DELETE FROM users;
DELETE FROM test_access_token WHERE id = 1;

INSERT INTO users (id, username, password, phone, nickname, avatar_url, email, github_id, github_username,
                   oauth_provider, oauth_provider_id, status, deleted, version)
VALUES (20001, 't_user_20001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        '13900000001', 'Test User 1', 'https://example.com/u1.png', 't_user_20001@example.com',
        NULL, NULL, NULL, NULL, 1, 0, 0),
       (24001, 't_admin_24001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        '13900010001', 'Test Admin 1', NULL, NULL,
        NULL, NULL, NULL, NULL, 1, 0, 0),
       (30001, 't_merchant_30001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        '13900020001', 'Merchant Owner', 'https://example.com/u2.png', 't_merchant_30001@example.com',
        NULL, NULL, NULL, NULL, 1, 0, 0);

INSERT INTO user_profile_ext (id, user_id, gender, birthday, bio, country, province, city, personal_tags, preferences, deleted, version)
VALUES (21001, 20001, 'MALE', '1995-01-01', 'test profile', 'China', 'Shanghai', 'Shanghai',
        JSON_ARRAY('vip', 'tech'), JSON_OBJECT('lang', 'zh-CN'), 0, 0);

INSERT INTO user_address (id, user_id, address_tag, receiver_name, receiver_phone, country, province, city, district, street,
                          detail_address, postal_code, longitude, latitude, is_default, deleted, version)
VALUES (22001, 20001, 'HOME', 'Test User 1', '13900000001', 'China', 'Shanghai', 'Shanghai', 'Pudong', 'Century Ave',
        'No.200', '200120', 121.4737000, 31.2304000, 1, 0, 0);

INSERT INTO user_favorite (id, user_id, spu_id, sku_id, favorite_status, deleted, version)
VALUES (23001, 20001, 40001, 41001, 'ACTIVE', 0, 0);

INSERT INTO admin (id, username, password, real_name, phone, role, status, deleted, version)
VALUES (24001, 't_admin_24001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Admin 1', '13900010001', 'ADMIN', 1, 0, 0);

INSERT INTO sys_role (id, role_name, role_code, role_status, deleted, version)
VALUES (26001, 'User', 'ROLE_USER', 1, 0, 0),
       (26002, 'Merchant', 'ROLE_MERCHANT', 1, 0, 0),
       (26003, 'Admin', 'ROLE_ADMIN', 1, 0, 0),
       (26011, 'Super Admin', 'ROLE_SUPER_ADMIN', 1, 0, 0),
       (26012, 'Ops Admin', 'ROLE_OPS_ADMIN', 1, 0, 0);

INSERT INTO sys_permission (id, permission_name, permission_code, http_method, api_path, deleted, version)
VALUES (27001, 'Order Manage', 'order:manage', 'POST', '/api/v2/order-sub/*/ship', 0, 0),
       (27002, 'After Sale Audit', 'after_sale:audit', 'POST', '/api/v2/after-sales/*/audit', 0, 0),
       (27003, 'Product Publish', 'product:publish', 'POST', '/api/v2/spu/*/publish', 0, 0);

INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, route_path, sort_order, visible, deleted, version)
VALUES (28001, 'Order Center', 'MENU_ORDER_CENTER', 0, 'MENU', '/ops/orders', 1, 1, 0, 0),
       (28002, 'After Sale Center', 'MENU_AFTER_SALE_CENTER', 0, 'MENU', '/ops/after-sales', 2, 1, 0, 0),
       (28003, 'Product Center', 'MENU_PRODUCT_CENTER', 0, 'MENU', '/ops/products', 3, 1, 0, 0);

INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version)
VALUES (29001, 26011, 27001, 0, 0),
       (29002, 26011, 27002, 0, 0),
       (29003, 26011, 27003, 0, 0),
       (29004, 26012, 27001, 0, 0);

INSERT INTO sys_role_menu (id, role_id, menu_id, deleted, version)
VALUES (29101, 26011, 28001, 0, 0),
       (29102, 26011, 28002, 0, 0),
       (29103, 26011, 28003, 0, 0),
       (29104, 26012, 28001, 0, 0);

INSERT INTO sys_user_role (id, user_id, role_id, deleted, version)
VALUES (29201, 24001, 26003, 0, 0),
       (29202, 24001, 26011, 0, 0),
       (29203, 20001, 26001, 0, 0),
       (29204, 30001, 26001, 0, 0),
       (29205, 30001, 26002, 0, 0);

INSERT INTO merchant (id, username, password, merchant_name, phone, status, deleted, version)
VALUES (30001, 't_merchant_30001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Merchant A', '13900020001', 1, 0, 0);

INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status, auth_remark, deleted, version)
VALUES (25001, 30001, 'BL-TEST-30001', 'https://example.com/license-30001.png',
        'https://example.com/idf-30001.png', 'https://example.com/idb-30001.png',
        '13900020001', 'Shanghai Test Street 1', 1, 'approved', 0, 0);

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1);
