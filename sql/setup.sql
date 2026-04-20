-- =============================================================
-- TravelReservationSystemProject – Database Setup Script
-- =============================================================
-- Run this script as a privileged MySQL user (e.g. root) once
-- before starting the application.
--
-- IMPORTANT: Replace <YOUR_STRONG_PASSWORD> with a secure password
-- before running this script. Never use the placeholder value in
-- production.

-- 1. Create the database (if it does not already exist)
CREATE DATABASE IF NOT EXISTS testproject;

-- 2. Create the application user and grant access
--    Replace <YOUR_STRONG_PASSWORD> with a strong, unique password.
CREATE USER IF NOT EXISTS 'testuser'@'localhost' IDENTIFIED BY '<YOUR_STRONG_PASSWORD>';
GRANT ALL PRIVILEGES ON testproject.* TO 'testuser'@'localhost';
FLUSH PRIVILEGES;

-- 3. Switch to the application database
USE testproject;

-- 4. Create the users table
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);
