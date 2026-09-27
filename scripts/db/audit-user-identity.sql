-- Read-only preflight for the first identity migration.
-- A clean result contains no rows. Run against user_db before enabling Flyway in production.

SELECT 'user_roles_missing_user' AS issue, ur.id AS record_id
FROM user_roles ur
LEFT JOIN users u ON u.id = ur.user_id
WHERE ur.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'user_roles_missing_role', ur.id
FROM user_roles ur
LEFT JOIN roles r ON r.id = ur.role_id
WHERE ur.deleted = 0 AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'role_permissions_missing_role', rp.id
FROM role_permissions rp
LEFT JOIN roles r ON r.id = rp.role_id
WHERE rp.deleted = 0 AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'role_permissions_missing_permission', rp.id
FROM role_permissions rp
LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE rp.deleted = 0 AND (p.id IS NULL OR p.deleted <> 0)
UNION ALL
SELECT 'admin_missing_legacy_principal', a.id
FROM admin a
LEFT JOIN users u ON u.id = a.id
WHERE a.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'merchant_missing_owner_principal', m.id
FROM merchant m
LEFT JOIN users u ON u.id = m.owner_user_id
WHERE m.deleted = 0 AND (u.id IS NULL OR u.deleted <> 0)
UNION ALL
SELECT 'admin_username_mismatch', a.id
FROM admin a
JOIN users u ON u.id = a.id
WHERE a.deleted = 0 AND u.deleted = 0 AND a.username <> u.username
UNION ALL
SELECT 'merchant_username_mismatch', m.id
FROM merchant m
JOIN users u ON u.id = m.owner_user_id
WHERE m.deleted = 0 AND u.deleted = 0 AND m.username <> u.username;
