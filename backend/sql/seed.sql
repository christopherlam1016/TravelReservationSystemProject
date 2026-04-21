USE reservation_system;

INSERT INTO users (user, password, role)
VALUES ('admin', 'admin', 'admin')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

INSERT INTO users (user, password, role)
VALUES ('test', 'group16', 'customer')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

INSERT INTO users (user, password, role)
VALUES ('rep1', 'rep1', 'rep')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);
