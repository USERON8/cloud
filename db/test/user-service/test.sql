USE user_db;

DELETE FROM inbox_consume_log;
DELETE FROM outbox_event;
DELETE FROM user_favorite;
DELETE FROM user_profile_ext;
DELETE FROM operation_audit_log;
DELETE FROM user_address;
DELETE FROM merchant_auth;
DELETE FROM merchant;
DELETE FROM admin;
DELETE FROM users;
DELETE FROM test_access_token WHERE id = 1;

INSERT INTO users (id, username, phone, nickname, avatar_url, email, status, deleted, version)
VALUES (20001, 't_user_20001', '13900000001', 'Test User 1', 'https://example.com/u1.png',
        't_user_20001@example.com', 1, 0, 0),
       (24001, 't_admin_24001', '13900010001', 'Test Admin 1', NULL, NULL, 1, 0, 0),
       (30001, 't_merchant_30001', '13900020001', 'Merchant Owner', 'https://example.com/u2.png',
        't_merchant_30001@example.com', 1, 0, 0);

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

INSERT INTO merchant (id, username, merchant_name, phone, status, deleted, version)
VALUES (30001, 't_merchant_30001', 'Test Merchant A', '13900020001', 1, 0, 0);

INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status, auth_remark, deleted, version)
VALUES (25001, 30001, 'BL-TEST-30001', 'https://example.com/license-30001.png',
        'https://example.com/idf-30001.png', 'https://example.com/idb-30001.png',
        '13900020001', 'Shanghai Test Street 1', 1, 'approved', 0, 0);

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1);
