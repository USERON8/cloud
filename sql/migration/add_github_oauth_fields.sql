-- ==================== GitHub OAuth 字段迁移脚本 ====================
-- 为用户表添加GitHub OAuth登录支持字段
-- 执行时间: 2025-09-20
-- 描述: 支持GitHub OAuth2.1登录功能

USE `user_db`;

-- 添加GitHub OAuth相关字段
ALTER TABLE `users`
    ADD COLUMN `github_id` BIGINT UNSIGNED NULL COMMENT 'GitHub用户ID（OAuth登录专用）' AFTER `email`,
    ADD COLUMN `github_username` VARCHAR(100) NULL COMMENT 'GitHub用户名（OAuth登录专用）' AFTER `github_id`,
    ADD COLUMN `oauth_provider` VARCHAR(20) NULL COMMENT 'OAuth提供商（github, wechat等）' AFTER `github_username`,
    ADD COLUMN `oauth_provider_id` VARCHAR(100) NULL COMMENT 'OAuth提供商用户ID' AFTER `oauth_provider`;

-- 添加索引以提高查询性能
ALTER TABLE `users`
    ADD INDEX `idx_github_id` (`github_id`),
    ADD INDEX `idx_github_username` (`github_username`),
    ADD INDEX `idx_oauth_provider` (`oauth_provider`),
    ADD INDEX `idx_oauth_provider_combined` (`oauth_provider`, `oauth_provider_id`);

-- 添加唯一约束确保GitHub用户的唯一性
ALTER TABLE `users`
    ADD UNIQUE INDEX `uk_github_id` (`github_id`),
    ADD UNIQUE INDEX `uk_github_username` (`github_username`);

-- 创建复合唯一索引，确保同一OAuth提供商的用户ID唯一
ALTER TABLE `users`
    ADD UNIQUE INDEX `uk_oauth_provider_id` (`oauth_provider`, `oauth_provider_id`);

-- 验证字段添加成功
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'user_db' 
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME IN ('github_id', 'github_username', 'oauth_provider', 'oauth_provider_id')
ORDER BY ORDINAL_POSITION;

-- 验证索引创建成功
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'user_db' 
  AND TABLE_NAME = 'users'
  AND INDEX_NAME IN ('idx_github_id', 'idx_github_username', 'idx_oauth_provider', 
                     'idx_oauth_provider_combined', 'uk_github_id', 'uk_github_username', 'uk_oauth_provider_id')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 迁移完成提示
SELECT '🎉 GitHub OAuth字段迁移完成！' as status;
