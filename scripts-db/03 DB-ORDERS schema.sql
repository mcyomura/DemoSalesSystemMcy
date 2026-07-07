-- 1. Database creation (Schema) and app user
CREATE DATABASE db_orders;

-- Creates application user and sets its passwd (identified by)
CREATE USER IF NOT EXISTS 'appOrderService'@'%' IDENTIFIED BY 'tfdtjdnie922%';
-- CRUD grants only
GRANT SELECT, INSERT, UPDATE, DELETE ON db_orders.* TO 'appOrderService'@'%';
-- Apply privileges
FLUSH PRIVILEGES;

USE db_orders;

-- 2. Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;

-- 1. Sales Order Header Table
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    uuid VARCHAR(50) UNIQUE NOT NULL,
    status INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    inventory_status INT,
    payment_status INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    price_updated_at DATETIME NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

-- 2. Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL, -- Only ID from catalog-service
    quantity INT NOT NULL,
    price_at_purchase DECIMAL(10,2) NOT NULL, -- Price must be frozen from the time of purchase
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);