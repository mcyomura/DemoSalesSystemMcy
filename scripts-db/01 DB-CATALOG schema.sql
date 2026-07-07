-- 1. Database creation (Schema) and app user
CREATE DATABASE db_catalog;

-- Creates application user and sets its passwd (identified by)
CREATE USER IF NOT EXISTS 'appCatalogService'@'%' IDENTIFIED BY 'tfdtjdnie921%';
-- CRUD grants only
GRANT SELECT, INSERT, UPDATE, DELETE ON db_catalog.* TO 'appCatalogService'@'%';
-- Apply privileges
FLUSH PRIVILEGES;

USE db_catalog; --

-- 2. Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;

-- 3. Create Suppliers Table
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    tax_id VARCHAR(20) NOT NULL UNIQUE, -- Government Tax ID
    contact_email VARCHAR(100),
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create Products Table (Now linking to Supplier)
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,  -- Stock Keeping Unit
    supplier_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- 5. Create Inventory Table (Separated for advanced tracking)
CREATE TABLE inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE, -- UNIQUE guarantees a 1:1 relationship here
    quantity INT NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(100), -- E.g., 'Aisle 3, Shelf B'
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
);