USE reservation_system;

INSERT INTO users (user, password)
VALUES ('test', 'group16')
ON DUPLICATE KEY UPDATE password = VALUES(password);
