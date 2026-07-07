-- 1. Database creation (Schema) and app user
CREATE DATABASE db_payment;

-- Creates application user and sets its passwd (identified by)
CREATE USER IF NOT EXISTS 'appPaymentService'@'%' IDENTIFIED BY 'tfdtjdnie923%';
-- CRUD grants only
GRANT SELECT, INSERT, UPDATE, DELETE ON db_payment.* TO 'appPaymentService'@'%';
-- Apply privileges
FLUSH PRIVILEGES;

USE db_payment;

-- 2. Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS payments;

-- 1. Sales Order Header Table
CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_token VARCHAR(255) NOT NULL,
    status INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);