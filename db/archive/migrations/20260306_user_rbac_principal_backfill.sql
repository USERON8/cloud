-- Backfill unified login principals and role mappings after removing users.user_type.
-- Target: MySQL 8.x / existing user_db deployments upgraded from pre-RBAC schema.

INSERT INTO user_db.sys_role (id, role_name, role_code, role_status, deleted, version)
VALUES (26001, 'User', 'ROLE_USER', 1, 0, 0),
       (26002, 'Merchant', 'ROLE_MERCHANT', 1, 0, 0),
       (26003, 'Admin', 'ROLE_ADMIN', 1, 0, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_code = VALUES(role_code),
    role_status = VALUES(role_status),
    deleted = VALUES(deleted),
    version = VALUES(version);

INSERT INTO user_db.users
    (id, username, password, phone, nickname, avatar_url, email, github_id, github_username,
     oauth_provider, oauth_provider_id, status, created_at, updated_at, deleted, version)
SELECT a.id,
       a.username,
       a.password,
       a.phone,
       a.real_name,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       a.status,
       a.created_at,
       a.updated_at,
       a.deleted,
       a.version
FROM user_db.admin a
WHERE NOT EXISTS (
    SELECT 1
    FROM user_db.users u
    WHERE u.id = a.id OR u.username = a.username
);

INSERT INTO user_db.users
    (id, username, password, phone, nickname, avatar_url, email, github_id, github_username,
     oauth_provider, oauth_provider_id, status, created_at, updated_at, deleted, version)
SELECT m.id,
       m.username,
       m.password,
       m.phone,
       m.merchant_name,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       m.status,
       m.created_at,
       m.updated_at,
       m.deleted,
       m.version
FROM user_db.merchant m
WHERE NOT EXISTS (
    SELECT 1
    FROM user_db.users u
    WHERE u.id = m.id OR u.username = m.username
);

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + u.id * 10 + 1,
       u.id,
       r.id,
       0,
       0
FROM user_db.users u
JOIN user_db.sys_role r ON r.role_code = 'ROLE_USER'
WHERE EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS c
    WHERE c.TABLE_SCHEMA = 'user_db'
      AND c.TABLE_NAME = 'users'
      AND c.COLUMN_NAME = 'user_type'
)
  AND u.user_type IN ('USER', 'MERCHANT');

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + u.id * 10 + 2,
       u.id,
       r.id,
       0,
       0
FROM user_db.users u
JOIN user_db.sys_role r ON r.role_code = 'ROLE_MERCHANT'
WHERE EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS c
    WHERE c.TABLE_SCHEMA = 'user_db'
      AND c.TABLE_NAME = 'users'
      AND c.COLUMN_NAME = 'user_type'
)
  AND u.user_type = 'MERCHANT';

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + u.id * 10 + 3,
       u.id,
       r.id,
       0,
       0
FROM user_db.users u
JOIN user_db.sys_role r ON r.role_code = 'ROLE_ADMIN'
WHERE EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS c
    WHERE c.TABLE_SCHEMA = 'user_db'
      AND c.TABLE_NAME = 'users'
      AND c.COLUMN_NAME = 'user_type'
)
  AND u.user_type = 'ADMIN';

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + a.id * 10 + 4,
       u.id,
       r.id,
       0,
       0
FROM user_db.admin a
JOIN user_db.users u ON u.username = a.username
JOIN user_db.sys_role r ON r.role_code = 'ROLE_ADMIN';

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + m.id * 10 + 5,
       u.id,
       r.id,
       0,
       0
FROM user_db.merchant m
JOIN user_db.users u ON u.username = m.username
JOIN user_db.sys_role r ON r.role_code = 'ROLE_USER';

INSERT IGNORE INTO user_db.sys_user_role (id, user_id, role_id, deleted, version)
SELECT 900000000000 + m.id * 10 + 6,
       u.id,
       r.id,
       0,
       0
FROM user_db.merchant m
JOIN user_db.users u ON u.username = m.username
JOIN user_db.sys_role r ON r.role_code = 'ROLE_MERCHANT';

SET @drop_user_type_index_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'user_db'
              AND TABLE_NAME = 'users'
              AND INDEX_NAME = 'idx_users_user_type_deleted'
        ),
        'ALTER TABLE user_db.users DROP INDEX idx_users_user_type_deleted',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_user_type_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_user_type_column_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'user_db'
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'user_type'
        ),
        'ALTER TABLE user_db.users DROP COLUMN user_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_user_type_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
