-- 1. Database creation (Schema) and app user
CREATE DATABASE db_customer_auth;

-- Creates application user and sets its passwd (identified by)
CREATE USER IF NOT EXISTS 'appCustomerAuthService'@'%' IDENTIFIED BY 'tfdtjdnie924%';
-- CRUD grants only
GRANT SELECT, INSERT, UPDATE, DELETE ON db_customer_auth.* TO 'appCustomerAuthService'@'%';
-- Apply privileges
FLUSH PRIVILEGES;

USE db_customer_auth;

-- 2. Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS users;

-- Table structure for storing customer users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    full_name VARCHAR(255),
    github_id VARCHAR(100) NOT NULL UNIQUE,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME
);