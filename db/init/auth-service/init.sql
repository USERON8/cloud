DROP DATABASE IF EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS auth_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

CREATE TABLE IF NOT EXISTS auth_user
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL,
    password          VARCHAR(255) NOT NULL,
    status            TINYINT      NOT NULL DEFAULT 1,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    version           INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_auth_user_username_deleted (username, deleted),
    INDEX idx_auth_user_deleted (deleted),
    INDEX idx_auth_user_status_deleted (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_oauth_account
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    user_id           BIGINT UNSIGNED NOT NULL,
    provider          VARCHAR(32)     NOT NULL,
    provider_user_id  VARCHAR(100)    NOT NULL,
    provider_username VARCHAR(100)    NULL,
    email             VARCHAR(100)    NULL,
    avatar_url        VARCHAR(255)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_auth_oauth_provider_user_deleted (provider, provider_user_id, deleted),
    UNIQUE KEY uk_auth_oauth_user_provider_deleted (user_id, provider, deleted),
    INDEX idx_auth_oauth_user_deleted (user_id, deleted),
    INDEX idx_auth_oauth_deleted_id (deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    role_name         VARCHAR(64) NOT NULL,
    role_code         VARCHAR(64) NOT NULL,
    role_status       TINYINT     NOT NULL DEFAULT 1,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT     NOT NULL DEFAULT 0,
    version           INT         NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_role_code_deleted (role_code, deleted),
    INDEX idx_sys_role_status_deleted (role_status, deleted),
    INDEX idx_sys_role_deleted_id (deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_permission
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    permission_name   VARCHAR(64)  NOT NULL,
    permission_code   VARCHAR(128) NOT NULL,
    http_method       VARCHAR(16)  NULL,
    api_path          VARCHAR(255) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    version           INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_permission_code_deleted (permission_code, deleted),
    INDEX idx_sys_permission_deleted_id (deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_role
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    user_id           BIGINT UNSIGNED NOT NULL,
    role_id           BIGINT UNSIGNED NOT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_user_role_user_role_deleted (user_id, role_id, deleted),
    INDEX idx_sys_user_role_user_deleted (user_id, deleted),
    INDEX idx_sys_user_role_role_deleted (role_id, deleted),
    INDEX idx_sys_user_role_deleted_id (deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id                BIGINT UNSIGNED PRIMARY KEY,
    role_id           BIGINT UNSIGNED NOT NULL,
    permission_id     BIGINT UNSIGNED NOT NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0,
    version           INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_role_permission_role_permission_deleted (role_id, permission_id, deleted),
    INDEX idx_sys_role_permission_role_deleted (role_id, deleted),
    INDEX idx_sys_role_permission_permission_deleted (permission_id, deleted),
    INDEX idx_sys_role_permission_deleted_id (deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO sys_role (id, role_name, role_code, role_status, deleted, version)
VALUES (26001, 'User', 'ROLE_USER', 1, 0, 0),
       (26002, 'Merchant', 'ROLE_MERCHANT', 1, 0, 0),
       (26003, 'Admin', 'ROLE_ADMIN', 1, 0, 0),
       (26011, 'Super Admin', 'ROLE_SUPER_ADMIN', 1, 0, 0),
       (26012, 'Ops Admin', 'ROLE_OPS_ADMIN', 1, 0, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_code = VALUES(role_code),
    role_status = VALUES(role_status),
    deleted = VALUES(deleted),
    version = VALUES(version);

INSERT INTO sys_permission (id, permission_name, permission_code, http_method, api_path, deleted, version)
VALUES (27001, 'Read', 'read', NULL, NULL, 0, 0),
       (27002, 'Write', 'write', NULL, NULL, 0, 0),
       (27003, 'Delete', 'delete', NULL, NULL, 0, 0),
       (27011, 'Admin Read', 'admin:read', NULL, NULL, 0, 0),
       (27012, 'Admin Write', 'admin:write', NULL, NULL, 0, 0),
       (27021, 'User Read', 'user:read', NULL, NULL, 0, 0),
       (27022, 'User Write', 'user:write', NULL, NULL, 0, 0),
       (27023, 'User Delete', 'user:delete', NULL, NULL, 0, 0),
       (27031, 'Merchant Read', 'merchant:read', NULL, NULL, 0, 0),
       (27032, 'Merchant Write', 'merchant:write', NULL, NULL, 0, 0),
       (27041, 'Product Read', 'product:read', NULL, NULL, 0, 0),
       (27042, 'Product Write', 'product:write', NULL, NULL, 0, 0),
       (27043, 'Product Delete', 'product:delete', NULL, NULL, 0, 0),
       (27051, 'Order Read', 'order:read', NULL, NULL, 0, 0),
       (27052, 'Order Write', 'order:write', NULL, NULL, 0, 0),
       (27053, 'Order Delete', 'order:delete', NULL, NULL, 0, 0),
       (27061, 'Payment Read', 'payment:read', NULL, NULL, 0, 0),
       (27062, 'Payment Write', 'payment:write', NULL, NULL, 0, 0),
       (27071, 'Stock Read', 'stock:read', NULL, NULL, 0, 0),
       (27072, 'Stock Write', 'stock:write', NULL, NULL, 0, 0),
       (27081, 'Search Read', 'search:read', NULL, NULL, 0, 0),
       (27082, 'Search Write', 'search:write', NULL, NULL, 0, 0),
       (27091, 'Log Read', 'log:read', NULL, NULL, 0, 0),
       (27092, 'Log Write', 'log:write', NULL, NULL, 0, 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_code = VALUES(permission_code),
    http_method = VALUES(http_method),
    api_path = VALUES(api_path),
    deleted = VALUES(deleted),
    version = VALUES(version);

INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version)
VALUES (28001, 26001, 27001, 0, 0),
       (28002, 26001, 27002, 0, 0),
       (28003, 26001, 27021, 0, 0),
       (28004, 26001, 27022, 0, 0),
       (28005, 26001, 27051, 0, 0),
       (28006, 26001, 27052, 0, 0),
       (28011, 26002, 27001, 0, 0),
       (28012, 26002, 27002, 0, 0),
       (28013, 26002, 27021, 0, 0),
       (28014, 26002, 27022, 0, 0),
       (28015, 26002, 27031, 0, 0),
       (28016, 26002, 27032, 0, 0),
       (28017, 26002, 27041, 0, 0),
       (28018, 26002, 27042, 0, 0),
       (28019, 26002, 27071, 0, 0),
       (28020, 26002, 27072, 0, 0),
       (28021, 26002, 27051, 0, 0),
       (28022, 26002, 27052, 0, 0),
       (28031, 26003, 27001, 0, 0),
       (28032, 26003, 27002, 0, 0),
       (28033, 26003, 27003, 0, 0),
       (28034, 26003, 27011, 0, 0),
       (28035, 26003, 27012, 0, 0),
       (28036, 26003, 27021, 0, 0),
       (28037, 26003, 27022, 0, 0),
       (28038, 26003, 27023, 0, 0),
       (28039, 26003, 27041, 0, 0),
       (28040, 26003, 27042, 0, 0),
       (28041, 26003, 27043, 0, 0),
       (28042, 26003, 27051, 0, 0),
       (28043, 26003, 27052, 0, 0),
       (28044, 26003, 27053, 0, 0),
       (28045, 26003, 27061, 0, 0),
       (28046, 26003, 27062, 0, 0),
       (28047, 26003, 27071, 0, 0),
       (28048, 26003, 27072, 0, 0),
       (28049, 26003, 27081, 0, 0),
       (28050, 26003, 27082, 0, 0),
       (28051, 26003, 27091, 0, 0),
       (28052, 26003, 27092, 0, 0),
       (28101, 26011, 27001, 0, 0),
       (28102, 26011, 27002, 0, 0),
       (28103, 26011, 27003, 0, 0),
       (28104, 26011, 27011, 0, 0),
       (28105, 26011, 27012, 0, 0),
       (28106, 26011, 27021, 0, 0),
       (28107, 26011, 27022, 0, 0),
       (28108, 26011, 27023, 0, 0),
       (28109, 26011, 27041, 0, 0),
       (28110, 26011, 27042, 0, 0),
       (28111, 26011, 27043, 0, 0),
       (28112, 26011, 27051, 0, 0),
       (28113, 26011, 27052, 0, 0),
       (28114, 26011, 27053, 0, 0),
       (28115, 26011, 27061, 0, 0),
       (28116, 26011, 27062, 0, 0),
       (28117, 26011, 27071, 0, 0),
       (28118, 26011, 27072, 0, 0),
       (28119, 26011, 27081, 0, 0),
       (28120, 26011, 27082, 0, 0),
       (28121, 26011, 27091, 0, 0),
       (28122, 26011, 27092, 0, 0),
       (28131, 26012, 27001, 0, 0),
       (28132, 26012, 27002, 0, 0),
       (28133, 26012, 27041, 0, 0),
       (28134, 26012, 27042, 0, 0),
       (28135, 26012, 27051, 0, 0),
       (28136, 26012, 27052, 0, 0),
       (28137, 26012, 27061, 0, 0),
       (28138, 26012, 27062, 0, 0),
       (28139, 26012, 27071, 0, 0),
       (28140, 26012, 27072, 0, 0),
       (28141, 26012, 27081, 0, 0),
       (28142, 26012, 27082, 0, 0)
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id),
    deleted = VALUES(deleted),
    version = VALUES(version);
