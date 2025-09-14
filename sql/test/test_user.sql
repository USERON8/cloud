-- ==================== 用户数据库测试数据 (user_db) ====================
USE `user_db`;

-- 插入测试用户数据
INSERT INTO users (id, username, password, phone, nickname, avatar_url, email, user_type)
VALUES (1, 'user1', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13800000001', '测试用户1',
        'https://example.com/avatar1.jpg', 'user1@example.com', 'USER'),
       (2, 'user2', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13800000002', '测试用户2',
        'https://example.com/avatar2.jpg', 'user2@example.com', 'USER'),
       (3, 'merchant1', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13800000003', '商家用户1',
        'https://example.com/avatar3.jpg', 'merchant1@example.com', 'MERCHANT'),
       (4, 'admin1', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '13800000004', '管理员用户1',
        'https://example.com/avatar4.jpg', 'admin1@example.com', 'ADMIN');

-- 插入测试用户地址数据
INSERT INTO user_address (id, user_id, consignee, phone, province, city, district, street, detail_address, is_default)
VALUES (1, 1, '测试用户1', '13800000001', '北京市', '北京市', '朝阳区', '三里屯街道', 'SOHO现代城A座1001室', 1),
       (2, 1, '测试用户1(公司)', '13800000001', '上海市', '上海市', '浦东新区', '陆家嘴街道', '环球金融中心2001室', 0),
       (3, 2, '测试用户2', '13800000002', '广东省', '深圳市', '南山区', '科技园南路', '腾讯大厦101室', 1);

-- 插入测试管理员数据
INSERT INTO admin (id, username, password, real_name, phone, role)
VALUES (1, 'admin', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '超级管理员', '13800000000',
        'ADMIN'),
       (2, 'admin2', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '普通管理员', '13800000005',
        'ADMIN');

-- 插入测试商家数据
INSERT INTO merchant (id, username, password, merchant_name, phone)
VALUES (1, 'merchant1', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '测试商家1', '13800000003'),
       (2, 'merchant2', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', '测试商家2', '13800000006');

-- 插入测试商家认证数据
INSERT INTO merchant_auth (id, merchant_id, business_license_number, business_license_url, id_card_front_url,
                           id_card_back_url, contact_phone, contact_address, auth_status)
VALUES (1, 1, 'BL123456789012345678', 'https://example.com/license1.jpg', 'https://example.com/id_front1.jpg',
        'https://example.com/id_back1.jpg', '13800000003', '北京市朝阳区某某街道1001号', 1),
       (2, 2, 'BL987654321098765432', 'https://example.com/license2.jpg', 'https://example.com/id_front2.jpg',
        'https://example.com/id_back2.jpg', '13800000006', '上海市浦东新区某某街道2002号', 1);

-- 插入测试商家结算账户数据
INSERT INTO merchant_settlement_account (id, merchant_id, account_name, account_number, account_type, bank_name,
                                         is_default, status)
VALUES (1, 1, '测试商家1对公账户', '6222001234567890123', 1, '工商银行', 1, 1),
       (2, 2, '测试商家2对私账户', '6222009876543210987', 2, '建设银行', 1, 1);