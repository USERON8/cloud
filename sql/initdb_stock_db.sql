-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: stock_db
-- ------------------------------------------------------
-- Server version	8.0.42
drop database if exists stock_db;
create database stock_db;
use stock_db;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `stock_log`
--
DROP TABLE IF EXISTS `stock_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_log`
(
    `id`           bigint  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`   bigint  NOT NULL COMMENT '商品ID',
    `order_id`     bigint       DEFAULT NULL COMMENT '订单ID',
    `change_type`  tinyint NOT NULL COMMENT '变更类型：1-入库，2-出库，3-冻结，4-解冻',
    `change_count` int     NOT NULL COMMENT '变更数量',
    `before_count` int     NOT NULL COMMENT '变更前数量',
    `after_count`  int     NOT NULL COMMENT '变更后数量',
    `remark`       varchar(200) DEFAULT NULL COMMENT '备注',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='库存变更记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_log`
--

LOCK TABLES `stock_log` WRITE;
/*!40000 ALTER TABLE `stock_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `stock_log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_stock`
--

DROP TABLE IF EXISTS `tb_stock`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_stock`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`      bigint       NOT NULL COMMENT '商品ID',
    `product_name`    varchar(100) NOT NULL COMMENT '商品名称',
    `stock_count`     int          NOT NULL DEFAULT '0' COMMENT '库存数量',
    `frozen_count`    int          NOT NULL DEFAULT '0' COMMENT '冻结库存数量',
    `available_count` int GENERATED ALWAYS AS ((`stock_count` - `frozen_count`)) VIRTUAL COMMENT '可用库存数量',
    `version`         int          NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
    `create_time`     datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    KEY `idx_product_name` (`product_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 304
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='库存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_stock`
--

LOCK TABLES `tb_stock` WRITE;
/*!40000 ALTER TABLE `tb_stock`
    DISABLE KEYS */;
INSERT INTO `tb_stock` (`id`, `product_id`, `product_name`, `stock_count`, `frozen_count`, `version`, `create_time`,
                        `update_time`)
VALUES (204, 1001, 'iPhone 15 Pro Max 256GB', 50, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (205, 1002, 'iPhone 15 Pro 128GB', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (206, 1003, 'iPhone 15 Plus 256GB', 60, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (207, 1004, 'iPhone 15 128GB', 120, 15, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (208, 1005, 'iPad Air 第5代 64GB', 30, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (209, 1006, 'iPad Pro 11英寸 128GB', 25, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (210, 1007, 'MacBook Air M2 256GB', 40, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (211, 1008, 'MacBook Pro 14英寸 512GB', 20, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (212, 1009, 'Apple Watch Series 9 45mm', 100, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (213, 1010, 'AirPods Pro 第2代', 150, 20, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (214, 1011, '华为Mate 60 Pro 256GB', 45, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (215, 1012, '华为P60 Pro 128GB', 70, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (216, 1013, '小米14 Pro 256GB', 85, 11, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (217, 1014, '小米13 Ultra 512GB', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (218, 1015, 'OPPO Find X6 Pro 256GB', 55, 7, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (219, 1016, 'vivo X100 Pro 256GB', 65, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (220, 1017, '三星Galaxy S24 Ultra 256GB', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (221, 1018, '荣耀Magic5 Pro 256GB', 50, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (222, 1019, '一加12 256GB', 60, 7, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (223, 1020, '真我GT5 Pro 256GB', 75, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (224, 1021, '联想ThinkPad X1 Carbon', 25, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (225, 1022, '戴尔XPS 13 Plus', 30, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (226, 1023, '华硕ROG幻16', 20, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (227, 1024, 'Surface Laptop 5', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (228, 1025, 'MacBook Pro 16英寸 1TB', 15, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (229, 1026, '小米笔记本Pro 14', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (230, 1027, '华为MateBook X Pro', 28, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (231, 1028, '荣耀MagicBook 16', 45, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (232, 1029, '机械革命蛟龙16K', 22, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (233, 1030, '神舟战神Z8', 18, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (234, 1031, 'Sony WH-1000XM5', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (235, 1032, 'Bose QuietComfort 45', 60, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (236, 1033, 'AirPods Max', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (237, 1034, '森海塞尔HD660S', 25, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (238, 1035, '拜亚动力DT990 Pro', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (239, 1036, 'Audio-Technica ATH-M50x', 55, 7, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (240, 1037, '小米降噪耳机Pro', 90, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (241, 1038, '华为FreeBuds Pro 3', 110, 15, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (242, 1039, 'OPPO Enco X2', 75, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (243, 1040, 'vivo TWS 3 Pro', 85, 11, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (244, 1041, 'iPad Pro 12.9英寸 256GB', 20, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (245, 1042, 'iPad mini 第6代 256GB', 45, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (246, 1043, 'Surface Pro 9', 30, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (247, 1044, '华为MatePad Pro 12.6', 25, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (248, 1045, '小米平板6 Pro', 50, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (249, 1046, '荣耀平板V8 Pro', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (250, 1047, '联想小新Pad Pro 12.7', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (251, 1048, 'OPPO Pad Air', 60, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (252, 1049, 'vivo Pad2', 55, 7, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (253, 1050, '三星Galaxy Tab S9+', 28, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (254, 1051, 'Apple Watch Ultra 2', 40, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (255, 1052, 'Apple Watch SE 第2代', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (256, 1053, '华为Watch GT 4', 70, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (257, 1054, '小米Watch S3', 90, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (258, 1055, 'OPPO Watch 3 Pro', 50, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (259, 1056, 'vivo Watch 3', 65, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (260, 1057, '荣耀Watch 4', 75, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (261, 1058, '真我Watch T1', 85, 11, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (262, 1059, '一加Watch 2', 45, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (263, 1060, 'Amazfit GTR 4', 95, 13, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (264, 1061, 'Nintendo Switch OLED', 60, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (265, 1062, 'PlayStation 5', 15, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (266, 1063, 'Xbox Series X', 20, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (267, 1064, 'Steam Deck 512GB', 25, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (268, 1065, 'ROG Ally Z1 Extreme', 18, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (269, 1066, '任天堂Switch Lite', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (270, 1067, 'PlayStation 5 Slim', 12, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (271, 1068, 'Xbox Series S', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (272, 1069, 'Meta Quest 3 128GB', 30, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (273, 1070, 'PICO 4 Enterprise', 22, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (274, 1071, '罗技MX Master 3S', 100, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (275, 1072, '雷蛇DeathAdder V3', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (276, 1073, '海盗船K95 RGB', 45, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (277, 1074, '樱桃MX Keys', 60, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (278, 1075, '罗技G Pro X Superlight', 70, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (279, 1076, '雷蛇黑寡妇V4', 55, 7, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (280, 1077, '海盗船暗影RGB', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (281, 1078, 'SteelSeries Apex Pro', 35, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (282, 1079, '罗技G915 TKL', 50, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (283, 1080, '雷蛇猎魂光蛛V3', 65, 8, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (284, 1081, '三星980 PRO 1TB', 120, 15, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (285, 1082, '西数SN850X 2TB', 80, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (286, 1083, '英睿达P5 Plus 1TB', 100, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (287, 1084, '金士顿KC3000 1TB', 90, 11, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (288, 1085, '海康威视C4000 1TB', 110, 14, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (289, 1086, '致钛PC005 1TB', 95, 12, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (290, 1087, '铠侠RC20 1TB', 85, 10, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (291, 1088, '影驰HOF PRO 1TB', 70, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (292, 1089, '七彩虹CN600 1TB', 75, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (293, 1090, '长江存储致钛PC005', 105, 13, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (294, 1091, 'RTX 4090 24GB', 8, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (295, 1092, 'RTX 4080 16GB', 15, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (296, 1093, 'RTX 4070 Ti 12GB', 25, 3, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (297, 1094, 'RTX 4070 12GB', 40, 5, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (298, 1095, 'RTX 4060 Ti 16GB', 50, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (299, 1096, 'RTX 4060 8GB', 70, 9, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (300, 1097, 'RX 7900 XTX 24GB', 12, 1, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (301, 1098, 'RX 7900 XT 20GB', 18, 2, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (302, 1099, 'RX 7800 XT 16GB', 30, 4, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56'),
       (303, 1100, 'RX 7700 XT 12GB', 45, 6, 0, '2025-07-21 15:43:56', '2025-07-21 15:43:56');
/*!40000 ALTER TABLE `tb_stock`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `undo_log`
--

DROP TABLE IF EXISTS `undo_log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `undo_log`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint       NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int          NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `undo_log`
--

LOCK TABLES `undo_log` WRITE;
/*!40000 ALTER TABLE `undo_log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `undo_log`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2025-07-22 16:10:13
