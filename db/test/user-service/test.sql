USE user_db;

DELETE FROM user_address WHERE id IN (10001, 10002, 10003);
DELETE FROM merchant_auth WHERE id IN (10001, 10002);
DELETE FROM merchant WHERE id IN (10001, 10002);
DELETE FROM admin WHERE id IN (10001, 10002);
DELETE FROM admin WHERE username IN ('t_admin_10001', 't_admin_10002');
DELETE FROM merchant WHERE username IN ('t_merchant_10001', 't_merchant_10002');
DELETE FROM users WHERE id IN (10001, 10002, 10003);
DELETE FROM users WHERE username IN ('t_user_10001', 't_user_10002', 't_merchant_owner');
DELETE FROM users WHERE email IN ('t_user_10001@example.com', 't_user_10002@example.com', 't_user_10003@example.com');
DELETE FROM users WHERE phone IN ('13900000001', '13900000003');
DELETE FROM users WHERE github_id = 91000002;
DELETE FROM test_access_token WHERE id = 1;

INSERT INTO users (id, username, password, phone, nickname, avatar_url, email, github_id, github_username,
                   oauth_provider, oauth_provider_id, status, user_type, deleted, version)
VALUES (10001, 't_user_10001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        '13900000001', 'Test User 1', 'https://example.com/u1.png', 't_user_10001@example.com',
        NULL, NULL, NULL, NULL, 1, 'USER', 0, 0),
       (10002, 't_user_10002', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        NULL, 'GitHub User', 'https://example.com/u2.png', 't_user_10002@example.com',
        91000002, 'gh_user_10002', 'github', '91000002', 1, 'USER', 0, 0),
       (10003, 't_merchant_owner', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        '13900000003', 'Merchant Owner', 'https://example.com/u3.png', 't_user_10003@example.com',
        NULL, NULL, NULL, NULL, 1, 'MERCHANT', 0, 0);

INSERT INTO user_address (id, user_id, consignee, phone, province, city, district, street, detail_address, is_default, deleted, version)
VALUES (10001, 10001, 'Test User 1', '13900000001', 'Beijing', 'Beijing', 'Chaoyang', 'Jianguo Rd', 'No.1001', 1, 0, 0),
       (10002, 10001, 'Test User 1 Office', '13900000001', 'Beijing', 'Beijing', 'Haidian', 'Zhongguancun', 'No.88', 0, 0, 0),
       (10003, 10002, 'GitHub User', '13900000002', 'Shanghai', 'Shanghai', 'Pudong', 'Century Ave', 'No.200', 1, 0, 0);

INSERT INTO admin (id, username, password, real_name, phone, role, status, deleted, version)
VALUES (10001, 't_admin_10001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Admin 1', '13900010001', 'ADMIN', 1, 0, 0),
       (10002, 't_admin_10002', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Admin 2', '13900010002', 'ADMIN', 1, 0, 0);

INSERT INTO merchant (id, username, password, merchant_name, phone, status, deleted, version)
VALUES (10001, 't_merchant_10001', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Merchant A', '13900020001', 1, 0, 0),
       (10002, 't_merchant_10002', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu',
        'Test Merchant B', '13900020002', 1, 0, 0);

INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status, auth_remark, deleted, version)
VALUES (10001, 10001, 'BL-TEST-10001', 'https://example.com/license-10001.png',
        'https://example.com/idf-10001.png', 'https://example.com/idb-10001.png',
        '13900020001', 'Beijing Test Street 1', 1, 'approved', 0, 0),
       (10002, 10002, 'BL-TEST-10002', 'https://example.com/license-10002.png',
        'https://example.com/idf-10002.png', 'https://example.com/idb-10002.png',
        '13900020002', 'Shanghai Test Street 2', 0, NULL, 0, 0);

INSERT INTO test_access_token (id, token_value, token_owner, expires_at, is_active)
VALUES (1, 'TEST_ENV_PERMANENT_TOKEN', 'test-user', '2099-12-31 23:59:59', 1);
