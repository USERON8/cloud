-- Enforce invariants that application-level CRUD cannot guarantee under concurrency.

ALTER TABLE user_profile_ext
    ADD COLUMN active_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id ELSE NULL END) STORED,
    DROP INDEX uk_user_profile_ext_user_id,
    ADD UNIQUE KEY uk_user_profile_ext_active_user (active_user_id),
    ADD CONSTRAINT fk_user_profile_ext_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_address
    ADD COLUMN active_default_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN deleted = 0 AND is_default = 1 THEN user_id ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY uk_user_address_active_default (active_default_user_id),
    ADD CONSTRAINT chk_user_address_is_default CHECK (is_default IN (0, 1)),
    ADD CONSTRAINT fk_user_address_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_favorite
    ADD COLUMN active_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id ELSE NULL END) STORED,
    ADD COLUMN active_spu_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN spu_id ELSE NULL END) STORED,
    ADD COLUMN active_sku_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN COALESCE(sku_id, 0) ELSE NULL END) STORED,
    DROP INDEX uk_user_favorite_user_spu_sku,
    ADD UNIQUE KEY uk_user_favorite_active (active_user_id, active_spu_id, active_sku_id),
    ADD CONSTRAINT fk_user_favorite_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE merchant_auth
    ADD COLUMN active_merchant_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN merchant_id ELSE NULL END) STORED,
    DROP INDEX uk_merchant_auth_merchant_id,
    ADD UNIQUE KEY uk_merchant_auth_active_merchant (active_merchant_id),
    ADD CONSTRAINT chk_merchant_auth_status CHECK (auth_status IN (0, 1, 2)),
    ADD CONSTRAINT fk_merchant_auth_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant (id);
