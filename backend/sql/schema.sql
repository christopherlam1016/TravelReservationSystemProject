/* LOGIN SCHEMA - temporary until we add relational schema from ER digram */
CREATE DATABASE IF NOT EXISTS reservation_system;
USE reservation_system;

/* create initial user and grant privileges */
CREATE USER IF NOT EXISTS 'test'@'localhost' IDENTIFIED BY 'group16';
ALTER USER 'test'@'localhost' IDENTIFIED BY 'group16';
GRANT ALL PRIVILEGES ON reservation_system.* TO 'test'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    user VARCHAR(100) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);
