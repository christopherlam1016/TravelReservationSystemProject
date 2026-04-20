* LOGIN SCHEMA * 
CREATE DATABASE IF NOT EXISTS testproject;
USE testproject;

CREATE USER IF NOT EXISTS 'testuser'@'localhost' IDENTIFIED BY 'test';
ALTER USER 'testuser'@'localhost' IDENTIFIED BY 'test';
GRANT ALL PRIVILEGES ON testproject.* TO 'testuser'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    user VARCHAR(100) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);
