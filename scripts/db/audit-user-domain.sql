-- Read-only preflight for V2__user_domain_invariants.sql.
-- A clean result contains no rows. Run against user_db before applying V2.

SELECT 'profile_missing_user' AS issue, profile.id AS record_id
FROM user_profile_ext profile
LEFT JOIN users u ON u.id = profile.user_id
WHERE profile.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'address_missing_user', address.id
FROM user_address address
LEFT JOIN users u ON u.id = address.user_id
WHERE address.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'favorite_missing_user', favorite.id
FROM user_favorite favorite
LEFT JOIN users u ON u.id = favorite.user_id
WHERE favorite.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'merchant_auth_missing_merchant', merchant_auth.id
FROM merchant_auth
LEFT JOIN merchant m ON m.id = merchant_auth.merchant_id
WHERE merchant_auth.deleted = 0 AND (m.id IS NULL OR m.deleted <> 0)
UNION ALL
SELECT 'multiple_active_default_addresses', MIN(address.id)
FROM user_address address
WHERE address.deleted = 0 AND address.is_default = 1
GROUP BY address.user_id
HAVING COUNT(*) > 1
UNION ALL
SELECT 'duplicate_active_favorite', MIN(favorite.id)
FROM user_favorite favorite
WHERE favorite.deleted = 0
GROUP BY favorite.user_id, favorite.spu_id, COALESCE(favorite.sku_id, 0)
HAVING COUNT(*) > 1;
