DELETE FROM process_activity
WHERE window_name IS NULL OR TRIM(window_name) = '';
