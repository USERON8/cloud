-- ==================== 商品数据库测试数据 (product_db) ====================
-- 版本: v2.0
-- 更新时间: 2025-01-16
-- 说明: 包含分类、店铺、品牌、商品、SKU、属性、审核、评价等测试数据
-- ==================================================================================
USE `product_db`;

-- 清空现有测试数据(按依赖顺序)
TRUNCATE TABLE `product_review`;
TRUNCATE TABLE `brand_authorization`;
TRUNCATE TABLE `product_audit`;
TRUNCATE TABLE `attribute_template`;
TRUNCATE TABLE `product_attribute`;
TRUNCATE TABLE `sku_specification`;
TRUNCATE TABLE `product_sku`;
TRUNCATE TABLE `products`;
TRUNCATE TABLE `brand`;
TRUNCATE TABLE `merchant_shop`;
TRUNCATE TABLE `category`;

-- ==================== 1. 商品分类测试数据 ====================
INSERT INTO `category` (id, parent_id, name, level, sort_order, status)
VALUES
(1, 0, '电子产品', 1, 1, 1),
(2, 0, '服装鞋帽', 1, 2, 1),
(3, 0, '食品饮料', 1, 3, 1),
(4, 1, '手机', 2, 1, 1),
(5, 1, '电脑', 2, 2, 1),
(6, 1, '数码配件', 2, 3, 1),
(7, 2, '男装', 2, 1, 1),
(8, 2, '女装', 2, 2, 1),
(9, 3, '零食', 2, 1, 1),
(10, 3, '饮料', 2, 2, 1);

-- ==================== 2. 品牌测试数据 ====================
INSERT INTO `brand` (id, brand_name, brand_name_en, logo_url, description, country, founded_year, status, is_hot, is_recommended, product_count, sort_order)
VALUES
(1, '苹果', 'Apple', 'https://example.com/brand/apple.png', '创新科技引领者', '美国', 1976, 1, 1, 1, 5, 1),
(2, '华为', 'HUAWEI', 'https://example.com/brand/huawei.png', '中国智造,联接未来', '中国', 1987, 1, 1, 1, 3, 2),
(3, '小米', 'Xiaomi', 'https://example.com/brand/xiaomi.png', '让每个人都享受科技的乐趣', '中国', 2010, 1, 1, 0, 2, 3),
(4, '优衣库', 'UNIQLO', 'https://example.com/brand/uniqlo.png', 'LifeWear服适人生', '日本', 1949, 1, 1, 1, 2, 4),
(5, 'ZARA', 'ZARA', 'https://example.com/brand/zara.png', '快时尚领导品牌', '西班牙', 1975, 1, 1, 0, 1, 5),
(6, '可口可乐', 'Coca-Cola', 'https://example.com/brand/cocacola.png', '开启欢乐,畅享美味', '美国', 1886, 1, 0, 1, 1, 6);

-- ==================== 3. 商家店铺测试数据 ====================
INSERT INTO `merchant_shop` (id, merchant_id, shop_name, avatar_url, description, contact_phone, address, status)
VALUES
(1, 1, '苹果官方旗舰店', 'https://example.com/shop/apple.jpg', '苹果官方授权店铺,品质保证', '400-666-8800', '北京市朝阳区建国路1号', 1),
(2, 2, '华为官方旗舰店', 'https://example.com/shop/huawei.jpg', '华为官方直营,正品保障', '400-830-8300', '深圳市龙岗区坂田华为基地', 1),
(3, 3, '优衣库官方旗舰店', 'https://example.com/shop/uniqlo.jpg', '优衣库官方店铺,服适人生', '400-820-1777', '上海市浦东新区陆家嘴环路1000号', 1),
(4, 4, '数码精品店', 'https://example.com/shop/digital.jpg', '精选数码产品,品质优选', '13800138001', '广州市天河区天河路123号', 1),
(5, 5, '时尚服饰店', 'https://example.com/shop/fashion.jpg', '时尚潮流,品味生活', '13800138002', '杭州市西湖区文三路456号', 1);

-- ==================== 4. 商品主表测试数据 ====================
INSERT INTO `products` (id, shop_id, product_name, product_description, price, original_price, stock_quantity, category_id, brand_id, status, sales_count, view_count)
VALUES
(1, 1, 'iPhone 15 Pro', '钛金属设计,A17 Pro芯片,专业级摄像系统', 7999.00, 8999.00, 100, 4, 1, 1, 1250, 15620),
(2, 1, 'MacBook Pro 14', 'M3 Pro芯片,14英寸Liquid视网膜XDR显示屏', 15999.00, 17999.00, 50, 5, 1, 1, 320, 8750),
(3, 2, '华为Mate 60 Pro', '卫星通信,强大影像系统,鸿蒙4.0', 6999.00, 7999.00, 80, 4, 2, 1, 890, 12350),
(4, 2, '华为MateBook X Pro', '触控屏,3K全面屏,超轻薄设计', 8999.00, 9999.00, 40, 5, 2, 1, 180, 5200),
(5, 3, '优衣库男士圆领T恤', '100%棉,舒适透气,多色可选', 99.00, 129.00, 500, 7, 4, 1, 3560, 28900),
(6, 3, '优衣库女士连衣裙', '纯棉面料,简约设计,显瘦版型', 299.00, 399.00, 300, 8, 4, 1, 1230, 18600),
(7, 4, 'AirPods Pro 2', '主动降噪,空间音频,无线充电', 1899.00, 1999.00, 200, 6, 1, 1, 780, 9800),
(8, 5, 'ZARA女士外套', '时尚设计,秋冬新款,百搭款式', 599.00, 899.00, 120, 8, 5, 1, 420, 5600),
(9, 1, 'iPad Air', 'M1芯片,10.9英寸视网膜显示屏', 4799.00, 5299.00, 60, 5, 1, 1, 560, 7200),
(10, 2, '华为智能手表GT 4', '健康管理,超长续航,精准定位', 1688.00, 1988.00, 150, 6, 2, 1, 680, 8900);

-- ==================== 5. 商品SKU测试数据 ====================
INSERT INTO `product_sku` (id, product_id, sku_code, sku_name, spec_values, price, original_price, cost_price, stock_quantity, sales_quantity, image_url, weight, barcode, status, sort_order)
VALUES
-- iPhone 15 Pro SKU
(1, 1, 'IP15P-TI-128', 'iPhone 15 Pro 钛金属 128GB', '{"颜色":"原色钛金属","容量":"128GB"}', 7999.00, 8999.00, 6500.00, 50, 620, 'https://example.com/sku/ip15p-ti-128.jpg', 187, '6941487212345', 1, 1),
(2, 1, 'IP15P-TI-256', 'iPhone 15 Pro 钛金属 256GB', '{"颜色":"原色钛金属","容量":"256GB"}', 8999.00, 9999.00, 7300.00, 30, 380, 'https://example.com/sku/ip15p-ti-256.jpg', 187, '6941487212346', 1, 2),
(3, 1, 'IP15P-BL-128', 'iPhone 15 Pro 蓝色钛金属 128GB', '{"颜色":"蓝色钛金属","容量":"128GB"}', 7999.00, 8999.00, 6500.00, 20, 250, 'https://example.com/sku/ip15p-bl-128.jpg', 187, '6941487212347', 1, 3),

-- MacBook Pro 14 SKU
(4, 2, 'MBP14-SG-512', 'MacBook Pro 14 深空灰 512GB', '{"颜色":"深空灰","存储":"512GB SSD","内存":"16GB"}', 15999.00, 17999.00, 13000.00, 30, 200, 'https://example.com/sku/mbp14-sg-512.jpg', 1600, '6941487223456', 1, 1),
(5, 2, 'MBP14-SG-1TB', 'MacBook Pro 14 深空灰 1TB', '{"颜色":"深空灰","存储":"1TB SSD","内存":"16GB"}', 17999.00, 19999.00, 14500.00, 20, 120, 'https://example.com/sku/mbp14-sg-1tb.jpg', 1600, '6941487223457', 1, 2),

-- 优衣库T恤 SKU
(6, 5, 'UNI-TS-WH-M', '优衣库T恤 白色 M', '{"颜色":"白色","尺码":"M"}', 99.00, 129.00, 45.00, 100, 890, 'https://example.com/sku/uni-ts-wh-m.jpg', 200, '6941487234567', 1, 1),
(7, 5, 'UNI-TS-WH-L', '优衣库T恤 白色 L', '{"颜色":"白色","尺码":"L"}', 99.00, 129.00, 45.00, 120, 980, 'https://example.com/sku/uni-ts-wh-l.jpg', 210, '6941487234568', 1, 2),
(8, 5, 'UNI-TS-BK-M', '优衣库T恤 黑色 M', '{"颜色":"黑色","尺码":"M"}', 99.00, 129.00, 45.00, 150, 920, 'https://example.com/sku/uni-ts-bk-m.jpg', 200, '6941487234569', 1, 3),
(9, 5, 'UNI-TS-BK-L', '优衣库T恤 黑色 L', '{"颜色":"黑色","尺码":"L"}', 99.00, 129.00, 45.00, 130, 770, 'https://example.com/sku/uni-ts-bk-l.jpg', 210, '6941487234570', 1, 4),

-- AirPods Pro 2 SKU
(10, 7, 'APP2-WHT', 'AirPods Pro 2 白色', '{"颜色":"白色"}', 1899.00, 1999.00, 1500.00, 200, 780, 'https://example.com/sku/app2-wht.jpg', 61, '6941487245678', 1, 1);

-- ==================== 6. SKU规格定义测试数据 ====================
INSERT INTO `sku_specification` (id, spec_name, spec_values, category_id, spec_type, is_required, sort_order, status, description)
VALUES
(1, '颜色', '["原色钛金属","蓝色钛金属","白色钛金属","黑色钛金属"]', 4, 1, 1, 1, 1, '手机颜色规格'),
(2, '容量', '["128GB","256GB","512GB","1TB"]', 4, 1, 1, 2, 1, '手机存储容量'),
(3, '内存', '["8GB","16GB","32GB","64GB"]', 5, 1, 1, 1, 1, '电脑内存规格'),
(4, '存储', '["256GB SSD","512GB SSD","1TB SSD","2TB SSD"]', 5, 1, 1, 2, 1, '电脑存储规格'),
(5, '尺码', '["XS","S","M","L","XL","XXL"]', 7, 1, 1, 1, 1, '男装尺码'),
(6, '尺码', '["XS","S","M","L","XL"]', 8, 1, 1, 1, 1, '女装尺码'),
(7, '颜色', '["白色","黑色","灰色","蓝色","红色"]', 0, 1, 0, 1, 1, '通用颜色规格');

-- ==================== 7. 商品属性测试数据 ====================
INSERT INTO `product_attribute` (id, product_id, attr_name, attr_value, attr_group, attr_type, is_filterable, is_list_visible, is_detail_visible, sort_order, unit)
VALUES
-- iPhone 15 Pro 属性
(1, 1, 'CPU型号', 'A17 Pro芯片', '基本参数', 1, 1, 1, 1, 1, NULL),
(2, 1, '屏幕尺寸', '6.1', '显示屏', 2, 1, 1, 1, 1, '英寸'),
(3, 1, '分辨率', '2556x1179', '显示屏', 1, 0, 0, 1, 2, NULL),
(4, 1, '后置摄像头', '4800万像素主摄+1200万像素超广角+1200万像素长焦', '摄像头', 1, 0, 1, 1, 1, NULL),
(5, 1, '电池容量', '3274', '电池信息', 2, 0, 0, 1, 1, 'mAh'),
(6, 1, '充电功率', '27', '电池信息', 2, 0, 0, 1, 2, 'W'),
(7, 1, '防水等级', 'IP68', '其他', 1, 1, 1, 1, 1, NULL),

-- MacBook Pro 14 属性
(8, 2, 'CPU型号', 'Apple M3 Pro芯片', '基本参数', 1, 1, 1, 1, 1, NULL),
(9, 2, '屏幕尺寸', '14.2', '显示屏', 2, 1, 1, 1, 1, '英寸'),
(10, 2, '分辨率', '3024x1964', '显示屏', 1, 0, 0, 1, 2, NULL),
(11, 2, '刷新率', '120', '显示屏', 2, 0, 1, 1, 3, 'Hz'),
(12, 2, '厚度', '15.5', '机身', 2, 0, 0, 1, 1, 'mm'),
(13, 2, '重量', '1.60', '机身', 2, 0, 1, 1, 2, 'kg'),

-- 优衣库T恤属性
(14, 5, '面料成分', '100%棉', '材质', 1, 1, 1, 1, 1, NULL),
(15, 5, '洗涤说明', '可机洗,不可漂白,低温熨烫', '洗护', 1, 0, 0, 1, 1, NULL),
(16, 5, '版型', '宽松', '设计', 1, 1, 1, 1, 1, NULL);

-- ==================== 8. 属性模板测试数据 ====================
INSERT INTO `attribute_template` (id, template_name, category_id, attributes, description, status, is_system, usage_count, creator_id)
VALUES
(1, '手机通用属性模板', 4, '[{"attr_name":"CPU型号","attr_type":1,"is_required":true},{"attr_name":"屏幕尺寸","attr_type":2,"unit":"英寸","is_required":true},{"attr_name":"后置摄像头","attr_type":1,"is_required":true},{"attr_name":"电池容量","attr_type":2,"unit":"mAh","is_required":true}]', '手机类商品通用属性', 1, 1, 15, 1),
(2, '笔记本电脑属性模板', 5, '[{"attr_name":"CPU型号","attr_type":1,"is_required":true},{"attr_name":"屏幕尺寸","attr_type":2,"unit":"英寸","is_required":true},{"attr_name":"内存","attr_type":1,"is_required":true},{"attr_name":"硬盘","attr_type":1,"is_required":true},{"attr_name":"重量","attr_type":2,"unit":"kg","is_required":true}]', '笔记本电脑通用属性', 1, 1, 8, 1),
(3, '服装通用属性模板', 7, '[{"attr_name":"面料成分","attr_type":1,"is_required":true},{"attr_name":"洗涤说明","attr_type":1,"is_required":true},{"attr_name":"版型","attr_type":1,"is_required":false}]', '服装类商品通用属性', 1, 1, 25, 1);

-- ==================== 9. 商品审核记录测试数据 ====================
INSERT INTO `product_audit` (id, product_id, product_name, merchant_id, merchant_name, audit_status, audit_type, submit_time, auditor_id, auditor_name, audit_time, audit_comment, reject_reason, product_snapshot, priority)
VALUES
-- 已通过的审核
(1, 1, 'iPhone 15 Pro', 1, '苹果官方旗舰店', 'APPROVED', 'CREATE', '2025-01-10 10:00:00', 101, '审核员张三', '2025-01-10 10:30:00', '商品信息完整,审核通过', NULL, '{"product_name":"iPhone 15 Pro","price":7999.00,"category_id":4}', 2),
(2, 5, '优衣库男士圆领T恤', 3, '优衣库官方旗舰店', 'APPROVED', 'CREATE', '2025-01-10 14:00:00', 102, '审核员李四', '2025-01-10 14:20:00', '商品信息完整,审核通过', NULL, '{"product_name":"优衣库男士圆领T恤","price":99.00}', 2),

-- 价格变更审核(已通过)
(3, 1, 'iPhone 15 Pro', 1, '苹果官方旗舰店', 'APPROVED', 'PRICE', '2025-01-12 09:00:00', 101, '审核员张三', '2025-01-12 09:15:00', '价格调整合理,审核通过', NULL, '{"old_price":8999.00,"new_price":7999.00}', 3),

-- 待审核
(4, 9, 'iPad Air', 1, '苹果官方旗舰店', 'PENDING', 'UPDATE', '2025-01-15 10:00:00', NULL, NULL, NULL, NULL, NULL, '{"product_name":"iPad Air","update_fields":["description","price"]}', 2),
(5, 10, '华为智能手表GT 4', 2, '华为官方旗舰店', 'PENDING', 'CREATE', '2025-01-15 11:30:00', NULL, NULL, NULL, NULL, NULL, '{"product_name":"华为智能手表GT 4","price":1688.00}', 2),

-- 已拒绝的审核
(6, 8, 'ZARA女士外套', 5, '时尚服饰店', 'REJECTED', 'CREATE', '2025-01-14 15:00:00', 103, '审核员王五', '2025-01-14 15:30:00', '审核不通过', '商品图片不清晰,商品描述不完整', '{"product_name":"ZARA女士外套","price":599.00}', 2),

-- 紧急审核(价格异常调整)
(7, 7, 'AirPods Pro 2', 4, '数码精品店', 'PENDING', 'PRICE', '2025-01-15 16:00:00', NULL, NULL, NULL, NULL, NULL, '{"old_price":1999.00,"new_price":1899.00}', 4);

-- ==================== 10. 品牌授权测试数据 ====================
INSERT INTO `brand_authorization` (id, brand_id, brand_name, merchant_id, merchant_name, auth_type, auth_status, certificate_url, start_time, end_time, auditor_id, auditor_name, audit_time, audit_comment, remark)
VALUES
-- 已授权
(1, 1, '苹果', 1, '苹果官方旗舰店', 'OFFICIAL', 'APPROVED', 'https://example.com/cert/apple-auth-001.pdf', '2024-01-01 00:00:00', '2026-12-31 23:59:59', 101, '授权审核员A', '2024-01-01 10:00:00', '官方旗舰店,授权通过', '苹果官方直营店铺'),
(2, 2, '华为', 2, '华为官方旗舰店', 'OFFICIAL', 'APPROVED', 'https://example.com/cert/huawei-auth-001.pdf', '2024-01-01 00:00:00', '2025-12-31 23:59:59', 101, '授权审核员A', '2024-01-01 10:00:00', '官方旗舰店,授权通过', '华为官方直营店铺'),
(3, 4, '优衣库', 3, '优衣库官方旗舰店', 'OFFICIAL', 'APPROVED', 'https://example.com/cert/uniqlo-auth-001.pdf', '2024-06-01 00:00:00', '2026-05-31 23:59:59', 102, '授权审核员B', '2024-06-01 09:00:00', '官方旗舰店,授权通过', '优衣库官方授权'),
(4, 1, '苹果', 4, '数码精品店', 'AUTHORIZED', 'APPROVED', 'https://example.com/cert/apple-auth-002.pdf', '2024-03-01 00:00:00', '2025-02-28 23:59:59', 101, '授权审核员A', '2024-03-01 14:00:00', '授权经销商,审核通过', '苹果授权经销商'),

-- 待审核
(5, 3, '小米', 4, '数码精品店', 'DISTRIBUTOR', 'PENDING', 'https://example.com/cert/xiaomi-auth-001.pdf', '2025-01-01 00:00:00', '2025-12-31 23:59:59', NULL, NULL, NULL, NULL, '小米分销商申请'),

-- 已拒绝
(6, 5, 'ZARA', 5, '时尚服饰店', 'AUTHORIZED', 'REJECTED', 'https://example.com/cert/zara-auth-001.pdf', NULL, NULL, 103, '授权审核员C', '2025-01-10 16:00:00', '授权证书不符合要求', '证书真实性存疑'),

-- 已过期
(7, 2, '华为', 4, '数码精品店', 'DISTRIBUTOR', 'EXPIRED', 'https://example.com/cert/huawei-auth-002.pdf', '2023-01-01 00:00:00', '2024-12-31 23:59:59', 102, '授权审核员B', '2023-01-01 10:00:00', '分销商授权', '授权已到期,需重新申请');

-- ==================== 11. 商品评价测试数据 ====================
INSERT INTO `product_review` (id, product_id, product_name, sku_id, order_id, order_no, user_id, user_nickname, user_avatar, rating, content, images, tags, is_anonymous, audit_status, audit_time, audit_comment, merchant_reply, reply_time, like_count, is_visible, review_type, parent_review_id)
VALUES
-- iPhone 15 Pro 评价
(1, 1, 'iPhone 15 Pro', 1, 1001, 'ORD20250111001', 10001, '科技达人小王', 'https://example.com/avatar/user1.jpg', 5, '非常棒的手机!A17 Pro芯片性能强劲,钛金属外壳手感一流,拍照效果惊艳!', '["https://example.com/review/img1.jpg","https://example.com/review/img2.jpg"]', '["外观漂亮","性能强劲","拍照清晰"]', 0, 'APPROVED', '2025-01-12 10:00:00', NULL, '感谢您的好评!我们会继续为您提供优质产品和服务。', '2025-01-12 14:00:00', 156, 1, 'INITIAL', NULL),

(2, 1, 'iPhone 15 Pro', 2, 1002, 'ORD20250112001', 10002, '数码爱好者', 'https://example.com/avatar/user2.jpg', 4, '整体不错,就是价格有点贵。续航表现中规中矩,日常使用一天没问题。', '["https://example.com/review/img3.jpg"]', '["续航一般","价格偏高"]', 0, 'APPROVED', '2025-01-13 09:00:00', NULL, '感谢您的反馈!我们会持续优化产品体验。', '2025-01-13 10:30:00', 89, 1, 'INITIAL', NULL),

-- 追加评价
(3, 1, 'iPhone 15 Pro', 2, 1002, 'ORD20250112001', 10002, '数码爱好者', 'https://example.com/avatar/user2.jpg', 5, '使用一周后追加评价:系统非常流畅,iOS 17体验很好,推荐购买!', NULL, '["系统流畅"]', 0, 'APPROVED', '2025-01-20 15:00:00', NULL, NULL, NULL, 23, 1, 'ADDITIONAL', 2),

-- MacBook Pro 评价
(4, 2, 'MacBook Pro 14', 4, 1003, 'ORD20250113001', 10003, '设计师小李', NULL, 5, 'M3 Pro芯片太强了!跑PS和PR毫无压力,屏幕色彩准确,续航给力!', '["https://example.com/review/img4.jpg","https://example.com/review/img5.jpg"]', '["性能强悍","屏幕优秀","续航长"]', 0, 'APPROVED', '2025-01-14 11:00:00', NULL, '感谢认可!祝您工作愉快!', '2025-01-14 16:00:00', 234, 1, 'INITIAL', NULL),

-- 优衣库T恤评价
(5, 5, '优衣库男士圆领T恤', 6, 1004, 'ORD20250114001', 10004, '***', 'https://example.com/avatar/default.jpg', 5, '纯棉材质,穿着很舒服,价格实惠,已经回购三次了!', NULL, '["面料舒适","性价比高"]', 1, 'APPROVED', '2025-01-15 10:00:00', NULL, '感谢您的支持,期待您再次光临!', '2025-01-15 11:00:00', 67, 1, 'INITIAL', NULL),

(6, 5, '优衣库男士圆领T恤', 8, 1005, 'ORD20250114002', 10005, '小张', 'https://example.com/avatar/user5.jpg', 4, '质量不错,就是黑色洗几次有点褪色。', NULL, '["质量好","有点褪色"]', 0, 'APPROVED', '2025-01-15 14:00:00', NULL, '感谢反馈!建议反面洗涤,避免阳光直晒。', '2025-01-15 15:30:00', 45, 1, 'INITIAL', NULL),

-- AirPods Pro 2 评价
(7, 7, 'AirPods Pro 2', 10, 1006, 'ORD20250113002', 10006, '音乐发烧友', 'https://example.com/avatar/user6.jpg', 5, '降噪效果非常好!音质比上一代提升明显,空间音频体验很棒!', '["https://example.com/review/img6.jpg"]', '["降噪优秀","音质好","空间音频"]', 0, 'APPROVED', '2025-01-14 16:00:00', NULL, NULL, NULL, 178, 1, 'INITIAL', NULL),

-- 待审核评价
(8, 3, '华为Mate 60 Pro', NULL, 1007, 'ORD20250115001', 10007, '华为粉丝', 'https://example.com/avatar/user7.jpg', 5, '支持国产!卫星通信功能很实用,信号好,拍照也很棒!', '["https://example.com/review/img7.jpg"]', '["支持国产","信号好"]', 0, 'PENDING', NULL, NULL, NULL, NULL, 0, 1, 'INITIAL', NULL),

-- 差评(已审核)
(9, 6, '优衣库女士连衣裙', NULL, 1008, 'ORD20250115002', 10008, '***', NULL, 2, '质量一般,颜色和图片有色差,不太满意。', NULL, '["有色差","质量一般"]', 1, 'APPROVED', '2025-01-16 09:00:00', NULL, '非常抱歉给您带来不好的体验,我们已记录您的反馈并改进。如需退换货请联系客服。', '2025-01-16 10:00:00', 12, 1, 'INITIAL', NULL),

-- 低分评价(被隐藏)
(10, 8, 'ZARA女士外套', NULL, 1009, 'ORD20250115003', 10009, '小红', 'https://example.com/avatar/user9.jpg', 1, '质量太差了,穿一次就开线了!', NULL, '["质量差"]', 0, 'APPROVED', '2025-01-16 11:00:00', '评价内容真实', NULL, NULL, 5, 0, 'INITIAL', NULL);

COMMIT;

-- ==================== 测试数据说明 ====================
--
-- 1. 分类层级:
--    - 一级分类: 电子产品(1)、服装鞋帽(2)、食品饮料(3)
--    - 二级分类: 手机(4)、电脑(5)、数码配件(6)、男装(7)、女装(8)、零食(9)、饮料(10)
--
-- 2. 品牌状态:
--    - 热门品牌(is_hot=1): 苹果、华为、小米、优衣库、ZARA
--    - 推荐品牌(is_recommended=1): 苹果、华为、优衣库、可口可乐
--
-- 3. 商品SKU场景:
--    - 多规格SKU: iPhone 15 Pro(3个SKU)、MacBook Pro 14(2个SKU)、优衣库T恤(4个SKU)
--    - 单规格SKU: AirPods Pro 2(1个SKU)
--
-- 4. 商品属性类型:
--    - 文本属性(attr_type=1): CPU型号、面料成分等
--    - 数字属性(attr_type=2): 屏幕尺寸、电池容量、重量等
--    - 可筛选属性(is_filterable=1): CPU型号、屏幕尺寸、防水等级等
--
-- 5. 审核状态场景:
--    - APPROVED: 审核通过(商品1、5的CREATE审核,商品1的PRICE审核)
--    - PENDING: 待审核(商品9的UPDATE审核、商品10的CREATE审核、商品7的PRICE审核)
--    - REJECTED: 审核拒绝(商品8的CREATE审核)
--
-- 6. 品牌授权状态:
--    - APPROVED: 已授权(苹果官方、华为官方、优衣库官方、苹果授权经销商)
--    - PENDING: 待审核(小米分销商申请)
--    - REJECTED: 已拒绝(ZARA授权申请)
--    - EXPIRED: 已过期(华为分销商授权)
--
-- 7. 商品评价场景:
--    - 高分好评(5星): 评价1、3、4、5、7、8
--    - 中等评价(4星): 评价2、6
--    - 低分差评(1-2星): 评价9、10
--    - 追加评价: 评价3(评价2的追加)
--    - 匿名评价: 评价5、9
--    - 带图评价: 评价1、2、4、7、8
--    - 商家回复: 评价1、2、4、5、6、9已回复
--    - 待审核: 评价8
--    - 已隐藏: 评价10(is_visible=0)
--
-- 8. 测试覆盖功能:
--    - 商品多规格SKU管理
--    - 商品属性和属性模板
--    - 商品审核流程(新建、更新、价格变更)
--    - 品牌授权管理
--    - 商品评价和追加评价
--    - 商家回复评价
--    - 评价点赞
--    - 匿名评价
--
