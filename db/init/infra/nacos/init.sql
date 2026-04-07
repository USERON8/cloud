-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: nacos
-- ------------------------------------------------------
-- Server version	8.0.42
drop database if exists nacos_config;
create database nacos_config;
use nacos_config;

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
-- Table structure for table `config_info`
--


DROP TABLE IF EXISTS `config_info`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info`
(
    `id`                 bigint                            NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`            varchar(255) COLLATE utf8mb3_bin  NOT NULL COMMENT 'data_id',
    `group_id`           varchar(128) COLLATE utf8mb3_bin           DEFAULT NULL COMMENT 'group_id',
    `content`            longtext COLLATE utf8mb3_bin      NOT NULL COMMENT 'content',
    `md5`                varchar(32) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'md5',
    `gmt_create`         datetime                          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`       datetime                          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`           text COLLATE utf8mb3_bin COMMENT 'source user',
    `src_ip`             varchar(50) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'source ip',
    `app_name`           varchar(128) COLLATE utf8mb3_bin           DEFAULT NULL COMMENT 'app_name',
    `tenant_id`          varchar(128) COLLATE utf8mb3_bin           DEFAULT '' COMMENT '租户字段',
    `c_desc`             varchar(256) COLLATE utf8mb3_bin           DEFAULT NULL COMMENT 'configuration description',
    `c_use`              varchar(64) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'configuration usage',
    `effect`             varchar(64) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT '配置生效的描述',
    `type`               varchar(64) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT '配置的类型',
    `c_schema`           text COLLATE utf8mb3_bin COMMENT '配置的模式',
    `encrypted_data_key` varchar(1024) COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '密钥',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`, `group_id`, `tenant_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='config_info';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info`
--

LOCK TABLES `config_info` WRITE;
/*!40000 ALTER TABLE `config_info`
    DISABLE KEYS */;
INSERT INTO `config_info`
VALUES (1, 'common.yaml', 'DEFAULT_GROUP',
        'DB_HOST: 127.0.0.1\r\nDB_PORT: 13306\r\nDB_USERNAME: root\r\nDB_PASSWORD: root\r\nREDIS_HOST: 127.0.0.1\r\nREDIS_PORT: 26379\r\nREDIS_PASSWORD: root\r\nROCKETMQ_NAMESRV_HOST: 127.0.0.1\r\nROCKETMQ_NAMESRV_PORT: 20011\r\nAUTH_HOST: 127.0.0.1\r\nAUTH_PORT: 8081\r\nELASTICSEARCH_URIS: http://127.0.0.1:19200\r\nMINIO_ENDPOINT: http://127.0.0.1:19000\r\nMINIO_PUBLIC_ENDPOINT: http://127.0.0.1:19000',
        NULL, '2025-07-21 13:39:22', '2025-07-21 13:39:22', 'nacos_namespace_migrate',
        '192.168.43.215', '', '', NULL, NULL, NULL, 'yaml', NULL, ''),
       (2, 'common.yaml', 'DEFAULT_GROUP',
        'DB_HOST: 127.0.0.1\r\nDB_PORT: 13306\r\nDB_USERNAME: root\r\nDB_PASSWORD: root\r\nREDIS_HOST: 127.0.0.1\r\nREDIS_PORT: 26379\r\nREDIS_PASSWORD: root\r\nROCKETMQ_NAMESRV_HOST: 127.0.0.1\r\nROCKETMQ_NAMESRV_PORT: 20011\r\nAUTH_HOST: 127.0.0.1\r\nAUTH_PORT: 8081\r\nELASTICSEARCH_URIS: http://127.0.0.1:19200\r\nMINIO_ENDPOINT: http://127.0.0.1:19000\r\nMINIO_PUBLIC_ENDPOINT: http://127.0.0.1:19000',
        NULL, '2025-07-21 13:39:22', '2025-07-21 13:39:22', 'nacos', '192.168.43.215', '',
        'public', NULL, NULL, NULL, 'yaml', NULL, ''),
       (3, 'gateway.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8080\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn\r\nCPOLAR_DOMAIN: http://6e314e0f.r6.cpolar.cn\r\nCPOLAR_PUBLIC_BASE_URL: http://6e314e0f.r6.cpolar.cn\r\nCPOLAR_FRONTEND_BASE_URL: http://6e314e0f.r6.cpolar.cn\r\nAPP_SECURITY_CORS_ALLOWED_ORIGIN_PATTERNS: http://127.0.0.1:*,https://127.0.0.1:*,http://localhost:*,https://localhost:*,http://6e314e0f.r6.cpolar.cn,https://6e314e0f.r6.cpolar.cn\r\nAPP_SENTINEL_GATEWAY_SEARCH_QPS: 120\r\nAPP_SECURITY_SEARCH_RATE_LIMIT_REPLENISH_RATE: 120\r\nAPP_SECURITY_SEARCH_RATE_LIMIT_BURST_CAPACITY: 240\r\nAPP_SECURITY_SEARCH_RATE_LIMIT_REQUESTED_TOKENS: 1\r\nGATEWAY_HTTPCLIENT_CONNECT_TIMEOUT_MS: 5000\r\nGATEWAY_HTTPCLIENT_RESPONSE_TIMEOUT: 20s\r\nAPP_REMOTE_HTTP_CONNECT_TIMEOUT_MS: 5000\r\nAPP_REMOTE_HTTP_RESPONSE_TIMEOUT_MS: 8000\r\nAPP_REMOTE_HTTP_READ_TIMEOUT_MS: 8000\r\nAPP_REMOTE_HTTP_WRITE_TIMEOUT_MS: 8000\r\nSEARCH_FALLBACK_TIMEOUT_MS: 2000',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'gateway custom config', 'service', 'yaml', NULL, ''),
       (4, 'auth-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8081\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn\r\nCPOLAR_PUBLIC_BASE_URL: http://6e314e0f.r6.cpolar.cn\r\nCPOLAR_FRONTEND_BASE_URL: http://6e314e0f.r6.cpolar.cn\r\nGITHUB_REDIRECT_URI: http://6e314e0f.r6.cpolar.cn/login/oauth2/code/github\r\nAPP_OAUTH2_GITHUB_ERROR_URL: http://6e314e0f.r6.cpolar.cn/auth/error\r\nAPP_OAUTH2_WEB_REDIRECT_URIS: http://6e314e0f.r6.cpolar.cn/callback,http://127.0.0.1:18080/callback,http://127.0.0.1:3000/callback,http://127.0.0.1:5173/callback,http://localhost:5173/callback',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'auth service custom config', 'service', 'yaml', NULL, ''),
       (5, 'user-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8082\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'user service custom config', 'service', 'yaml', NULL, ''),
       (6, 'product-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8084\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'product service custom config', 'service', 'yaml', NULL, ''),
       (7, 'order-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8085\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'order service custom config', 'service', 'yaml', NULL, ''),
       (8, 'payment-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8086\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn\r\nALIPAY_NOTIFY_URL: http://6e314e0f.r6.cpolar.cn/api/v1/payment/alipay/notify\r\nALIPAY_RETURN_URL: http://6e314e0f.r6.cpolar.cn/#/pages/app/payments/index',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'payment service custom config', 'service', 'yaml', NULL, ''),
       (9, 'search-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8087\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn\r\nSEARCH_ES_REQUEST_TIMEOUT_MS: 1500',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'search service custom config', 'service', 'yaml', NULL, ''),
       (10, 'stock-service.yaml', 'DEFAULT_GROUP',
        'SERVICE_LOCAL_URL: http://127.0.0.1:8088\r\nSERVICE_VIEW_URL: http://127.0.0.1:18080\r\nSERVICE_PUBLIC_URL: http://6e314e0f.r6.cpolar.cn',
        NULL, '2026-04-07 20:20:00', '2026-04-07 20:20:00', 'codex', '127.0.0.1', '',
        'public', 'stock service custom config', 'service', 'yaml', NULL, '');
/*!40000 ALTER TABLE `config_info`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info_gray`
--

DROP TABLE IF EXISTS `config_info_gray`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info_gray`
(
    `id`                 bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`            varchar(255)    NOT NULL COMMENT 'data_id',
    `group_id`           varchar(128)    NOT NULL COMMENT 'group_id',
    `content`            longtext        NOT NULL COMMENT 'content',
    `md5`                varchar(32)              DEFAULT NULL COMMENT 'md5',
    `src_user`           text COMMENT 'src_user',
    `src_ip`             varchar(100)             DEFAULT NULL COMMENT 'src_ip',
    `gmt_create`         datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_create',
    `gmt_modified`       datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_modified',
    `app_name`           varchar(128)             DEFAULT NULL COMMENT 'app_name',
    `tenant_id`          varchar(128)             DEFAULT '' COMMENT 'tenant_id',
    `gray_name`          varchar(128)    NOT NULL COMMENT 'gray_name',
    `gray_rule`          text            NOT NULL COMMENT 'gray_rule',
    `encrypted_data_key` varchar(256)    NOT NULL DEFAULT '' COMMENT 'encrypted_data_key',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfogray_datagrouptenantgray` (`data_id`, `group_id`, `tenant_id`, `gray_name`),
    KEY `idx_dataid_gmt_modified` (`data_id`, `gmt_modified`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3 COMMENT ='config_info_gray';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info_gray`
--

LOCK TABLES `config_info_gray` WRITE;
/*!40000 ALTER TABLE `config_info_gray`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `config_info_gray`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_tags_relation`
--

DROP TABLE IF EXISTS `config_tags_relation`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_tags_relation`
(
    `id`        bigint                           NOT NULL COMMENT 'id',
    `tag_name`  varchar(128) COLLATE utf8mb3_bin NOT NULL COMMENT 'tag_name',
    `tag_type`  varchar(64) COLLATE utf8mb3_bin  DEFAULT NULL COMMENT 'tag_type',
    `data_id`   varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
    `group_id`  varchar(128) COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_id',
    `nid`       bigint                           NOT NULL AUTO_INCREMENT COMMENT 'nid, 自增长标识',
    PRIMARY KEY (`nid`),
    UNIQUE KEY `uk_configtagrelation_configidtag` (`id`, `tag_name`, `tag_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='config_tag_relation';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_tags_relation`
--

LOCK TABLES `config_tags_relation` WRITE;
/*!40000 ALTER TABLE `config_tags_relation`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `config_tags_relation`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_capacity`
--

DROP TABLE IF EXISTS `group_capacity`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_capacity`
(
    `id`                bigint unsigned                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`          varchar(128) COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
    `quota`             int unsigned                     NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int unsigned                     NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int unsigned                     NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int unsigned                     NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
    `max_aggr_size`     int unsigned                     NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int unsigned                     NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime                         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime                         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='集群、各Group容量信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_capacity`
--

LOCK TABLES `group_capacity` WRITE;
/*!40000 ALTER TABLE `group_capacity`
    DISABLE KEYS */;
INSERT INTO `group_capacity`
VALUES (1, '', 0, 2, 0, 0, 0, 0, '2025-07-21 05:39:22', '2025-07-21 15:46:04');
/*!40000 ALTER TABLE `group_capacity`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `his_config_info`
--

DROP TABLE IF EXISTS `his_config_info`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `his_config_info`
(
    `id`                 bigint unsigned                   NOT NULL COMMENT 'id',
    `nid`                bigint unsigned                   NOT NULL AUTO_INCREMENT COMMENT 'nid, 自增标识',
    `data_id`            varchar(255) COLLATE utf8mb3_bin  NOT NULL COMMENT 'data_id',
    `group_id`           varchar(128) COLLATE utf8mb3_bin  NOT NULL COMMENT 'group_id',
    `app_name`           varchar(128) COLLATE utf8mb3_bin           DEFAULT NULL COMMENT 'app_name',
    `content`            longtext COLLATE utf8mb3_bin      NOT NULL COMMENT 'content',
    `md5`                varchar(32) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'md5',
    `gmt_create`         datetime                          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`       datetime                          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`           text COLLATE utf8mb3_bin COMMENT 'source user',
    `src_ip`             varchar(50) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'source ip',
    `op_type`            char(10) COLLATE utf8mb3_bin               DEFAULT NULL COMMENT 'operation type',
    `tenant_id`          varchar(128) COLLATE utf8mb3_bin           DEFAULT '' COMMENT '租户字段',
    `encrypted_data_key` varchar(1024) COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '密钥',
    `publish_type`       varchar(50) COLLATE utf8mb3_bin            DEFAULT 'formal' COMMENT 'publish type gray or formal',
    `gray_name`          varchar(50) COLLATE utf8mb3_bin            DEFAULT NULL COMMENT 'gray name',
    `ext_info`           longtext COLLATE utf8mb3_bin COMMENT 'ext info',
    PRIMARY KEY (`nid`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_did` (`data_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='多租户改造';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `his_config_info`
--

LOCK TABLES `his_config_info` WRITE;
/*!40000 ALTER TABLE `his_config_info`
    DISABLE KEYS */;
INSERT INTO `his_config_info`
VALUES (0, 1, 'common.yaml', 'DEFAULT_GROUP', '',
        'DB_HOST: 127.0.0.1\r\nDB_PORT: 13306\r\nDB_USERNAME: root\r\nDB_PASSWORD: root\r\nREDIS_HOST: 127.0.0.1\r\nREDIS_PORT: 26379\r\nREDIS_PASSWORD: root\r\nROCKETMQ_NAMESRV_HOST: 127.0.0.1\r\nROCKETMQ_NAMESRV_PORT: 20011\r\nAUTH_HOST: 127.0.0.1\r\nAUTH_PORT: 8081\r\nELASTICSEARCH_URIS: http://127.0.0.1:19200\r\nMINIO_ENDPOINT: http://127.0.0.1:19000\r\nMINIO_PUBLIC_ENDPOINT: http://127.0.0.1:19000',
        '21332872d9ea7eb086dc02cdf7797965', '2025-07-21 13:39:22', '2025-07-21 05:39:22', 'nacos', '192.168.43.215',
        'I', 'public', '', 'formal', '', '{\"src_user\":\"nacos\",\"type\":\"yaml\"}');
/*!40000 ALTER TABLE `his_config_info`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions`
(
    `role`     varchar(50)  NOT NULL COMMENT 'role',
    `resource` varchar(128) NOT NULL COMMENT 'resource',
    `action`   varchar(8)   NOT NULL COMMENT 'action',
    UNIQUE KEY `uk_role_permission` (`role`, `resource`, `action`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles`
(
    `username` varchar(50) NOT NULL COMMENT 'username',
    `role`     varchar(50) NOT NULL COMMENT 'role',
    UNIQUE KEY `idx_user_role` (`username`, `role`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles`
    DISABLE KEYS */;
INSERT INTO `roles`
VALUES ('nacos', 'ROLE_ADMIN');
/*!40000 ALTER TABLE `roles`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_capacity`
--

DROP TABLE IF EXISTS `tenant_capacity`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_capacity`
(
    `id`                bigint unsigned                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         varchar(128) COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT 'Tenant ID',
    `quota`             int unsigned                     NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int unsigned                     NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int unsigned                     NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int unsigned                     NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
    `max_aggr_size`     int unsigned                     NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int unsigned                     NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime                         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime                         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='租户容量信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_capacity`
--

LOCK TABLES `tenant_capacity` WRITE;
/*!40000 ALTER TABLE `tenant_capacity`
    DISABLE KEYS */;
INSERT INTO `tenant_capacity`
VALUES (1, 'public', 0, 1, 0, 0, 0, 0, '2025-07-21 05:39:22', '2025-07-21 15:46:04');
/*!40000 ALTER TABLE `tenant_capacity`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_info`
--

DROP TABLE IF EXISTS `tenant_info`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_info`
(
    `id`            bigint                           NOT NULL AUTO_INCREMENT COMMENT 'id',
    `kp`            varchar(128) COLLATE utf8mb3_bin NOT NULL COMMENT 'kp',
    `tenant_id`     varchar(128) COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_id',
    `tenant_name`   varchar(128) COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_name',
    `tenant_desc`   varchar(256) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'tenant_desc',
    `create_source` varchar(32) COLLATE utf8mb3_bin  DEFAULT NULL COMMENT 'create_source',
    `gmt_create`    bigint                           NOT NULL COMMENT '创建时间',
    `gmt_modified`  bigint                           NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`, `tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_bin COMMENT ='tenant_info';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_info`
--

LOCK TABLES `tenant_info` WRITE;
/*!40000 ALTER TABLE `tenant_info`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `tenant_info`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users`
(
    `username` varchar(50)  NOT NULL COMMENT 'username',
    `password` varchar(500) NOT NULL COMMENT 'password',
    `enabled`  tinyint(1)   NOT NULL COMMENT 'enabled',
    PRIMARY KEY (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users`
    DISABLE KEYS */;
INSERT INTO `users`
VALUES ('nacos', '$2a$10$m/b9ARBHKAop0eerterQV.x/FCa6zQ4Dg2LHNX/yzTySGWRREwfYu', 1);
/*!40000 ALTER TABLE `users`
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

-- Dump completed on 2025-07-22 16:09:54
