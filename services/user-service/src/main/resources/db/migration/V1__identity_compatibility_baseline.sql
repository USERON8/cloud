-- Approved direction: evolve the identity model in place. Keep legacy columns and APIs
-- available while new writes use explicit principal relationships and versioned auth state.

ALTER TABLE users
    ADD COLUMN auth_version BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Increment to invalidate previously issued credentials' AFTER status;

ALTER TABLE admin
    ADD COLUMN principal_id BIGINT UNSIGNED NULL COMMENT 'Explicit users.id login principal' AFTER id;

UPDATE admin
SET principal_id = id
WHERE principal_id IS NULL;

ALTER TABLE admin
    MODIFY COLUMN principal_id BIGINT UNSIGNED NOT NULL COMMENT 'Explicit users.id login principal',
    ADD UNIQUE KEY uk_admin_principal_id (principal_id);

ALTER TABLE users
    ADD COLUMN active_username VARCHAR(50)
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN username ELSE NULL END) STORED,
    DROP INDEX uk_users_username_deleted,
    ADD UNIQUE KEY uk_users_active_username (active_username),
    ADD CONSTRAINT chk_users_status CHECK (status IN (0, 1));

ALTER TABLE roles
    ADD COLUMN active_code VARCHAR(50)
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN code ELSE NULL END) STORED,
    DROP INDEX uk_roles_code_deleted,
    ADD UNIQUE KEY uk_roles_active_code (active_code);

ALTER TABLE permissions
    ADD COLUMN active_code VARCHAR(100)
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN code ELSE NULL END) STORED,
    DROP INDEX uk_permissions_code_deleted,
    ADD UNIQUE KEY uk_permissions_active_code (active_code);

ALTER TABLE user_roles
    ADD COLUMN active_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id ELSE NULL END) STORED,
    ADD COLUMN active_role_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN role_id ELSE NULL END) STORED,
    DROP INDEX uk_user_roles_user_role_deleted,
    ADD UNIQUE KEY uk_user_roles_active (active_user_id, active_role_id);

ALTER TABLE role_permissions
    ADD COLUMN active_role_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN role_id ELSE NULL END) STORED,
    ADD COLUMN active_permission_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN permission_id ELSE NULL END) STORED,
    DROP INDEX uk_role_permissions_role_permission_deleted,
    ADD UNIQUE KEY uk_role_permissions_active (active_role_id, active_permission_id);

ALTER TABLE admin
    ADD COLUMN active_username VARCHAR(50)
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN username ELSE NULL END) STORED,
    DROP INDEX uk_admin_username_deleted,
    ADD UNIQUE KEY uk_admin_active_username (active_username),
    ADD CONSTRAINT chk_admin_status CHECK (status IN (0, 1));

ALTER TABLE merchant
    ADD COLUMN active_username VARCHAR(50)
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN username ELSE NULL END) STORED,
    DROP INDEX uk_merchant_username_deleted,
    ADD UNIQUE KEY uk_merchant_active_username (active_username),
    ADD CONSTRAINT chk_merchant_status CHECK (status IN (0, 1)),
    ADD CONSTRAINT chk_merchant_audit_status CHECK (audit_status IN (0, 1, 2));

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE role_permissions
    ADD CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    ADD CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id);

ALTER TABLE admin
    ADD CONSTRAINT fk_admin_principal
        FOREIGN KEY (principal_id) REFERENCES users (id);

ALTER TABLE merchant
    ADD CONSTRAINT fk_merchant_owner
        FOREIGN KEY (owner_user_id) REFERENCES users (id);
