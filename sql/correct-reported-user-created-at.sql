UPDATE users
SET created_at = DATE_ADD(created_at, INTERVAL 330 MINUTE)
WHERE created_at = '2026-07-31 08:48:07';
